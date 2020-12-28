function api<T>(url: string): Promise<T> {
  return fetch(url)
    .then(response => {
      if (!response.ok) {
        throw new Error(response.statusText)
      }
      return response.json();
    })

}

function timeAgo(d: number) {
  const diffMs = ((new Date()).getTime() - d);
  const diffDays = Math.floor(diffMs / 86400000); // days
  const diffHrs = Math.floor((diffMs % 86400000) / 3600000); // hours
  const diffMins = Math.round(((diffMs % 86400000) % 3600000) / 60000); // minutes
  return (diffDays > 0 ? diffDays + "d " : "") + diffHrs + "h " + diffMins + "m";
}

function initMap(): void {
  const center = { lat: 52.331984, lng: 4.944248 };

  const map = new google.maps.Map(
    document.getElementById("map") as HTMLElement,
    {
      zoom: 16,
      center: center,
    }
  );

interface DevicePath {
  description: string,
  positions: Array<Position>
}

interface Position {
  latitude: number,
  longitude: number,
  time: string
}

const device = window.location.pathname.split("/")[1];
api<DevicePath>('/api/catracker/paths/' + device)
  .then(
     data => {
      new google.maps.Polyline({
           path: data.positions.map( (p: Position) => new google.maps.LatLng({ lat: p.latitude, lng: p.longitude }) ),
           geodesic: true,
           strokeColor: "#FF0000",
           strokeOpacity: 1.0,
           strokeWeight: 2,
           map: map
         });
       const last = data.positions[0];
       new google.maps.Marker( {
             position: {
               lat: last.latitude,
               lng: last.longitude,
             },
             label: device + ": " + timeAgo(Date.parse(last.time)),
             map: map
             } );
     }
  );
}
export { initMap };

import "./style.css";
