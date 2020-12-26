// import myData from 'data.json';

// Implementation code where T is the returned data shape
function api<T>(url: string): Promise<T> {
  return fetch(url)
    .then(response => {
      if (!response.ok) {
        throw new Error(response.statusText)
      }
      return response.json();
    })

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
  longitude: number
}

 api<DevicePath>('/api/catracker/paths/58A0CB0000204688')
  .then(
     data =>{
      if (data.positions) {
        new google.maps.Marker({
          position: {
            lat: data.positions[0].latitude,
            lng: data.positions[0].longitude,
          },
          map: map,
          title: "Last"});
      }
    });

}
export { initMap };

import "./style.css"; // required for webpack
