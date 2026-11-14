local garagePrefix = 'veicoliplugin:garage:'

local function getGarageKey(playerId)
    return garagePrefix .. tostring(playerId)
end

local function getGarageVehicles(playerId)
    local raw = GetResourceKvpString(getGarageKey(playerId))
    if not raw or raw == '' then
        return {}
    end

    local ok, decoded = pcall(function()
        return json.decode(raw)
    end)

    if not ok or type(decoded) ~= 'table' then
        return {}
    end

    return decoded
end

local function saveGarageVehicles(playerId, vehicles)
    SetResourceKvp(getGarageKey(playerId), json.encode(vehicles or {}))
end

local function findPlayerByName(name)
    local lowerName = string.lower(name)
    for _, id in ipairs(GetPlayers()) do
        local playerName = string.lower(GetPlayerName(id))
        if playerName == lowerName or string.find(playerName, lowerName, 1, true) then
            return tonumber(id)
        end
    end
    return nil
end

local function hasStaffAccess(source)
    if source == 0 then
        return true
    end

    local allowed = {
        'admin',
        'moderator',
        'staff',
        'headstaff',
        'owner'
    }

    for _, ace in ipairs(allowed) do
        if IsPlayerAceAllowed(source, 'group.' .. ace) then
            return true
        end
    end

    return IsPlayerAceAllowed(source, 'command.adminveicoli') or IsPlayerAceAllowed(source, 'command.garage')
end

local function normalizeVehicleName(name)
    if not name then
        return ''
    end

    return string.gsub(name, '^%s*(.-)%s*$', '%1')
end

local function containsVehicle(vehicles, vehicleName)
    for _, current in ipairs(vehicles) do
        if string.lower(tostring(current.name)) == string.lower(vehicleName) then
            return true
        end
    end
    return false
end

local function addVehicleToGarage(targetId, vehicleName)
    local vehicles = getGarageVehicles(targetId)
    local cleanedName = normalizeVehicleName(vehicleName)

    if cleanedName == '' then
        return false, 'Nome veicolo non valido.'
    end

    if containsVehicle(vehicles, cleanedName) then
        return false, 'Il veicolo è già nel garage.'
    end

    table.insert(vehicles, {
        name = cleanedName,
        model = string.lower(cleanedName:gsub('%s+', ''))
    })

    saveGarageVehicles(targetId, vehicles)
    return true, cleanedName
end

local function removeVehicleFromGarage(targetId, vehicleName)
    local vehicles = getGarageVehicles(targetId)
    local cleanedName = normalizeVehicleName(vehicleName)
    local newVehicles = {}

    for _, current in ipairs(vehicles) do
        if string.lower(tostring(current.name)) ~= string.lower(cleanedName) then
            table.insert(newVehicles, current)
        end
    end

    saveGarageVehicles(targetId, newVehicles)
    return true
end

local function getPlayerGarage(source, targetId)
    if targetId then
        return getGarageVehicles(targetId)
    end

    return getGarageVehicles(source)
end

RegisterCommand('garage', function(source, args)
    local targetId = nil
    if #args > 0 then
        if not hasStaffAccess(source) then
            TriggerClientEvent('chat:addMessage', source, {
                args = {'^1Errore', 'Non hai i permessi per aprire il garage di altri giocatori.'}
            })
            return
        end

        targetId = findPlayerByName(table.concat(args, ' '))
        if not targetId then
            TriggerClientEvent('chat:addMessage', source, {
                args = {'^1Errore', 'Player non trovato.'}
            })
            return
        end
    else
        targetId = source
    end

    local vehicles = getPlayerGarage(source, targetId)
    TriggerClientEvent('veicoliplugin:client:openGarage', source, vehicles, targetId)
end, false)

RegisterCommand('adminveicoli', function(source, args)
    if not hasStaffAccess(source) then
        TriggerClientEvent('chat:addMessage', source, {
            args = {'^1Errore', 'Non hai i permessi per usare /adminveicoli.'}
        })
        return
    end

    local action = args[1]
    local targetName = args[2]
    local vehicleName = table.concat(args, ' ', 3)

    if not action or (action ~= 'give' and action ~= 'remove') then
        TriggerClientEvent('chat:addMessage', source, {
            args = {'^1Uso', '/adminveicoli give [nick] [veicolo] | /adminveicoli remove [nick] [veicolo]'}
        })
        return
    end

    if not targetName or not vehicleName then
        TriggerClientEvent('chat:addMessage', source, {
            args = {'^1Uso', '/adminveicoli give [nick] [veicolo]'}
        })
        return
    end

    local targetId = findPlayerByName(targetName)
    if not targetId then
        TriggerClientEvent('chat:addMessage', source, {
            args = {'^1Errore', 'Player non trovato.'}
        })
        return
    end

    if action == 'give' then
        local ok, message = addVehicleToGarage(targetId, vehicleName)
        if ok then
            TriggerClientEvent('chat:addMessage', source, {
                args = {'^2Admin', 'Hai dato a ' .. GetPlayerName(targetId) .. ' il veicolo: ' .. message}
            })
            TriggerClientEvent('chat:addMessage', targetId, {
                args = {'^2Garage', 'Ti è stato dato il veicolo: ' .. message}
            })
        else
            TriggerClientEvent('chat:addMessage', source, {
                args = {'^1Errore', message}
            })
        end
    else
        removeVehicleFromGarage(targetId, vehicleName)
        TriggerClientEvent('chat:addMessage', source, {
            args = {'^2Admin', 'Hai rimosso da ' .. GetPlayerName(targetId) .. ' il veicolo: ' .. vehicleName}
        })
    end
end, false)

RegisterNetEvent('veicoliplugin:server:removeGarageVehicle', function(vehicleName)
    local sourceId = source
    local vehicles = getGarageVehicles(sourceId)
    local filtered = {}

    for _, current in ipairs(vehicles) do
        if string.lower(tostring(current.name)) ~= string.lower(tostring(vehicleName)) then
            table.insert(filtered, current)
        end
    end

    saveGarageVehicles(sourceId, filtered)
    TriggerClientEvent('veicoliplugin:client:garageUpdated', sourceId, filtered)
end)

RegisterNetEvent('veicoliplugin:server:requestGarageData', function()
    local sourceId = source
    TriggerClientEvent('veicoliplugin:client:openGarage', sourceId, getGarageVehicles(sourceId), sourceId)
end)

AddEventHandler('playerDropped', function(reason)
    -- Nessuna azione extra: i dati del garage rimangono persistenti.
end)

print('[veicoliplugin] Server loaded')
