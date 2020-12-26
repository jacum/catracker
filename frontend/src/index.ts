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

 api<DevicePath>('/paths.json')
  .then(
     data => data.positions.map(
     (p: Position) => {
        new google.maps.Marker( {
          position: {
            lat: p.latitude,
            lng: p.longitude,
          },
          map: map,
          label: p.time,
          } );

        }));

}
export { initMap };

import "./style.css"; // required for webpack
