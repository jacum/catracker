import {WebsocketBuilder, ExponentialBackoff} from 'websocket-ts';
import {v4 as uuidv4} from 'uuid';

interface DevicePath {
  description: string;
  lastSeen: number;
  positions: Array<Position>;
}

interface Position {
  latitude: number;
  longitude: number;
  battery: number;
}

function api<T>(url: string): Promise<T> {
  return fetch(url)
    .then(response => {
      if (!response.ok) {
        throw new Error(response.statusText)
      }
      return response.json();
    })
}

export function forceCast<T>(input: any): T {
  return input;
}

var currentData: DevicePath
var currentMarker:  google.maps.Marker
var currentPath:  google.maps.Polyline
var currentTimer: number = 0

function msToTime(duration: number) {
  if (duration < 60000) return "~";
  const  millisecondsValue: number = Math.floor((duration % 1000) / 100),
    secondsValue: number = Math.floor((duration / 1000) % 60),
    minutesValue: number = Math.floor((duration / (1000 * 60)) % 60),
    hoursValue: number = Math.floor((duration / (1000 * 60 * 60)) % 24);

  const hours = (hoursValue < 10) ? "0" + hoursValue : hoursValue;
  const minutes = (minutesValue < 10) ? "0" + minutesValue : minutesValue;
  const seconds = (secondsValue < 10) ? "0" + secondsValue : secondsValue;

    return hours > 0 ? hours + ":" + minutes + ":" + seconds : minutes + ":" + seconds;
}


function updateLabel(start: number, lastSeen: number, battery: string): void {
  const elapsed: number = lastSeen + (Date.now() - start);
  const batteryLabel = document.getElementById("battery") as HTMLElement;
  currentMarker.setLabel({ text: msToTime(elapsed), fontSize: "40px" });
  batteryLabel.textContent = battery;
}

function redraw(map: google.maps.Map, data: DevicePath, lastSeen: number): void {
  const last = data.positions[0];

  map.setCenter({ lat: last.latitude, lng: last.longitude});
  const battery = last.battery > 0 ? last.battery + "%" : "~";

  currentPath = new google.maps.Polyline({
       path: data.positions.map( (p: Position) => new google.maps.LatLng({ lat: p.latitude, lng: p.longitude }) ),
       geodesic: true,
       strokeColor: "#FF0000",
       strokeOpacity: 1.0,
       strokeWeight: 5,
       map: map
     });

   currentMarker = new google.maps.Marker( {
         position: {
           lat: last.latitude,
           lng: last.longitude,
         },
         opacity: 1,
         icon: {url: "/marker.png", labelOrigin: new google.maps.Point(40,90) },
         map: map
         }
       );
   clearInterval(currentTimer);
   const start: number = Date.now();
   updateLabel(start, lastSeen, battery);
   currentTimer = setInterval(
      () => {
        updateLabel(start, lastSeen, battery)
      },
      1000);
}

function initMap(): void {

  const device = window.location.pathname.split("/")[1];
  const location = window.location;

  var wsUrl: string;
  if (location.protocol === "https:") {
      wsUrl = "wss:";
  } else {
      wsUrl = "ws:";
  }
  const host = location.host;
//   const host = "localhost:8081";

  wsUrl += "//" + host + "/api/catracker/ws/" + device + "/" + uuidv4();
  const dataUrl = location.protocol + "//" + host + '/api/catracker/paths/' + device;
  api<DevicePath>(dataUrl)
    .then(
       data => {
          console.log(document.getElementById("map") )
          const map = new google.maps.Map(
           document.getElementById("map") as HTMLElement,
           {  zoom: 18 }
          );
          currentData = data;
          redraw(map, currentData, data.lastSeen);

          const ws = new WebsocketBuilder(wsUrl)
                .onOpen((i, ev) => { console.log("opened");  })
                .onClose((i, ev) => { console.log("closed") })
                .onError((i, ev) => { console.log("error") })
                .onMessage((i, ev) => {
                  if (ev.data != "") { // initial frame
                    const json = JSON.parse(ev.data)
                    console.log(json);
                    if (json.hasOwnProperty("Path")) {
                      console.log(json.Path.positions);
                      currentData.positions = json.Path.positions;
                      currentMarker.setMap(null);
                      currentPath.setMap(null);
                      redraw(map, currentData, 0);
                    }
                  }
                })
                .onRetry((i, ev) => { console.log("retry") })
                .withBackoff(new ExponentialBackoff(100, 7))
                .build();

       }
    );
}
export { initMap };

import "./style.css";
