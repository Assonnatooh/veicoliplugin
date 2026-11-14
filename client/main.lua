local garageVehicles = {}
local currentGear = 1
local hudVisible = false
local vehicleModelNames = {
    ['amgcross'] = 'AMG Cross',
    ['amg'] = 'AMG',
    ['sultan'] = 'Sultan',
    ['bati'] = 'Bati'
}

local gearLimits = {
    [1] = 15,
    [2] = 30,
    [3] = 55,
    [4] = 75,
    [5] = 110
}

local function sendHudData(speed, fuel, gear)
    SendNUIMessage({
        type = 'hud',
        speed = tostring(speed),
        fuel = tostring(fuel),
        gear = tostring(gear),
        visible = true
    })
end

local function hideHud()
    SendNUIMessage({
        type = 'hud',
        visible = false
    })
    hudVisible = false
end

local function normalizeVehicleName(vehicle)
    if type(vehicle) == 'table' and vehicle.name then
        return tostring(vehicle.name)
    end

    return tostring(vehicle or '')
end

local function openGarageUI(vehicles, targetId)
    garageVehicles = vehicles or {}
    local data = {
        vehicles = garageVehicles,
        targetId = targetId or GetPlayerServerId(PlayerId())
    }

    SetNuiFocus(true, true)
    SendNUIMessage({
        type = 'garage',
        action = 'open',
        vehicles = data.vehicles
    })
end

local function closeGarageUI()
    SetNuiFocus(false, false)
    SendNUIMessage({
        type = 'garage',
        action = 'close'
    })
end

local function updateGearBySpeed(speed)
    if speed <= gearLimits[1] then
        currentGear = 1
    elseif speed <= gearLimits[2] then
        currentGear = 2
    elseif speed <= gearLimits[3] then
        currentGear = 3
    elseif speed <= gearLimits[4] then
        currentGear = 4
    else
        currentGear = 5
    end
end

RegisterNetEvent('veicoliplugin:client:openGarage', function(vehicles, targetId)
    openGarageUI(vehicles, targetId)
end)

RegisterNetEvent('veicoliplugin:client:garageUpdated', function(vehicles)
    garageVehicles = vehicles or {}
    SendNUIMessage({
        type = 'garage',
        action = 'refresh',
        vehicles = garageVehicles
    })
end)

RegisterNUICallback('closeGarage', function(data, cb)
    closeGarageUI()
    cb('ok')
end)

RegisterNUICallback('removeVehicle', function(data, cb)
    local vehicleName = normalizeVehicleName(data and data.vehicleName or '')
    if vehicleName ~= '' then
        TriggerServerEvent('veicoliplugin:server:removeGarageVehicle', vehicleName)
    end
    cb('ok')
end)

RegisterCommand('veicoli_gear_up', function()
    if currentGear < 5 then
        currentGear = currentGear + 1
    end
end, false)

RegisterKeyMapping('veicoli_gear_up', 'Cambio marcia', 'keyboard', 'F')

Citizen.CreateThread(function()
    while true do
        local ped = PlayerPedId()
        local vehicle = GetVehiclePedIsIn(ped, false)

        if vehicle ~= 0 then
            local speed = math.floor(GetEntitySpeed(vehicle) * 3.6)
            local fuel = math.floor(GetVehicleFuelLevel(vehicle))

            if not hudVisible then
                hudVisible = true
            end

            updateGearBySpeed(speed)
            sendHudData(speed, fuel, currentGear)
        else
            hideHud()
        end

        Citizen.Wait(200)
    end
end)

print('[veicoliplugin] Client loaded')
