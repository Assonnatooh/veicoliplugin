const garage = document.getElementById('garage');
const garageList = document.getElementById('garage-list');
const closeGarageBtn = document.getElementById('closeGarage');
const vehicleHud = document.getElementById('vehicle-hud');

const speedValue = document.getElementById('speed-value');
const fuelValue = document.getElementById('fuel-value');
const gearValue = document.getElementById('gear-value');

closeGarageBtn.addEventListener('click', () => {
    fetch(`https://${GetParentResourceName()}/closeGarage`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=UTF-8' },
        body: JSON.stringify({})
    });
});

function renderGarage(vehicles) {
    garageList.innerHTML = '';

    if (!vehicles || vehicles.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'garage-item';
        empty.innerHTML = '<div class="garage-item-name">Nessun veicolo nel garage</div>';
        garageList.appendChild(empty);
        return;
    }

    vehicles.forEach((vehicle) => {
        const row = document.createElement('div');
        row.className = 'garage-item';

        const name = document.createElement('div');
        name.className = 'garage-item-name';
        name.textContent = vehicle.name || vehicle.model || 'Veicolo';

        const button = document.createElement('button');
        button.textContent = 'Rimuovi';
        button.addEventListener('click', () => {
            fetch(`https://${GetParentResourceName()}/removeVehicle`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                body: JSON.stringify({ vehicleName: vehicle.name || vehicle.model })
            });
        });

        row.appendChild(name);
        row.appendChild(button);
        garageList.appendChild(row);
    });
}

window.addEventListener('message', (event) => {
    const data = event.data;

    if (!data) return;

    if (data.type === 'garage') {
        if (data.action === 'open') {
            renderGarage(data.vehicles || []);
            garage.classList.remove('hidden');
        } else if (data.action === 'close') {
            garage.classList.add('hidden');
        } else if (data.action === 'refresh') {
            renderGarage(data.vehicles || []);
        }
    }

    if (data.type === 'hud') {
        if (data.visible === false) {
            vehicleHud.classList.add('hidden');
            return;
        }

        vehicleHud.classList.remove('hidden');
        speedValue.textContent = data.speed || '0';
        fuelValue.textContent = data.fuel || '0';
        gearValue.textContent = data.gear || '1';
    }
});
