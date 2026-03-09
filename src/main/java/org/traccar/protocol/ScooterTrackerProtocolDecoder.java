package org.traccar.protocol;

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
//import java.util.logging.Logger;


public class ScooterTrackerProtocolDecoder extends BaseProtocolDecoder {

    public ScooterTrackerProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ScooterTrackerProtocolDecoder.class);

    private void logDecodingError(long deviceID, String error)
    {
        String msg = "ScooterTrackerDecoder error by device: " + (Long.toString(deviceID)) + "-" + error;
        LOGGER.warn(msg);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        int index = 0;

        String sentence = (String) msg;
        String[] fields = sentence.split(";");

        //parsing deviceID
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, fields[index++]);
        if(deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        Network network = new Network();

        //parse STATUS
        switch (fields[index++])
        {
            case "N":
                position.set(Position.KEY_ARMED, false);
                break;
            case "A":
                position.set(Position.KEY_ARMED, true);
                break;
            case "S":
                position.set(Position.KEY_ARMED, true);
                position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                break;
            default:
                logDecodingError(position.getDeviceId(),"Status argument invalid");
                return null;
        }

        //parse date/time
        DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        position.setTime(dateFormat.parse(fields[index++]));

        //parse position validity
        switch (fields[index++])
        {
            case "T":
                position.setValid(true);
                break;
            case "F":
                position.setValid(false);
                break;
            default:
                logDecodingError(position.getDeviceId(), "Position validity invalid");
                return null;
        }

        position.setLatitude(Double.parseDouble(fields[index++]));      //parse latitude
        position.setLongitude(Double.parseDouble(fields[index++]));     //parse longitude
        position.setAltitude((Double.parseDouble((fields[index++]))));  //parse altitude
        position.setSpeed(UnitsConverter.knotsFromKph(Double.parseDouble(fields[index++]))); //parse speed       //parse speed
        position.setCourse(Integer.parseInt(fields[index++]));  //parse course

        position.set(Position.KEY_SATELLITES, Integer.parseInt(fields[index++]));   //parse number of satellites
        position.set(Position.KEY_RSSI, Integer.parseInt(fields[index++]));     //parse RSSI

        position.set(Position.KEY_BATTERY, Double.parseDouble(fields[index++]));    //parse battery level

        if(!(fields[index++].equals("C")))
        {
            logDecodingError(position.getDeviceId(), "Frame error by delimiter C");
            return null;
        }

        //parse mobile towers
        int cellCount = Integer.parseInt(fields[index++]);  //parse number of towers
        for (int i = 0; i < cellCount; i++) {
            network.addCellTower(CellTower.from(
                    Integer.parseInt(fields[index++], 10),  //mcc
                    Integer.parseInt(fields[index++], 16),  //mnc
                    Integer.parseInt(fields[index++], 16),  //lac
                    Integer.parseInt(fields[index++], 16),  //cid
                    Integer.parseInt(fields[index++], 10)));    //RSSI
        }

        if(!(fields[index++].equals("W")))
        {
            logDecodingError(position.getDeviceId(), "Frame error by delimiter W");
            return null;
        }

        int wifiCount = Integer.parseInt(fields[index++]);  //parse number of WIFI APs
        for (int i = 0; i < wifiCount; i++) {
            String mac = fields[index++];
            int rssi = Integer.parseInt((fields[index++]));

            if(mac.length() != 12)
            {
                logDecodingError(position.getDeviceId(), "WIFI MAC address length is not 12");
                return null;
            }

            //insert colon to MAC address
            String macFormatted = mac.substring(0,2) + ":" +
                    mac.substring(2,4) + ":" +
                    mac.substring(4,6) + ":" +
                    mac.substring(6,8) + ":" +
                    mac.substring(8,10) + ":" +
                    mac.substring(10,12);
            network.addWifiAccessPoint(WifiAccessPoint.from(macFormatted, rssi));
        }

        //parse motion
        switch (fields[index++])
        {
            case "M":
                position.set(Position.KEY_MOTION, true);
                break;
            case "S":
                position.set(Position.KEY_MOTION, false);
                break;
            default:
                logDecodingError(position.getDeviceId(),"Motion argument invalid");
                return null;
        }

        if(wifiCount > 0 || cellCount > 0)
        {
            position.setNetwork(network);
        }

        //get the latest position if it's currently not available
        if(!position.getValid())
        {
            getLastLocation(position, position.getDeviceTime());
        }

        return position;
    }

}
