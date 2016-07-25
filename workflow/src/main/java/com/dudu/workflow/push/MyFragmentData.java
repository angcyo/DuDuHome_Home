package com.dudu.workflow.push;

import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.InvalidFrameException;
import org.java_websocket.framing.FrameBuilder;
import org.java_websocket.framing.Framedata;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2016/5/24.
 */
public class MyFragmentData implements FrameBuilder {

    private boolean fin;

    private Opcode opcode;

    private ByteBuffer payload;

    private boolean transferemasked;

    @Override
    public void setFin(boolean fin) {
        this.fin = fin;
    }

    @Override
    public void setOptcode(Opcode optcode) {
        this.opcode = optcode;
    }

    @Override
    public void setPayload(ByteBuffer payload) throws InvalidDataException {
        this.payload = payload;
    }

    @Override
    public void setTransferemasked(boolean transferemasked) {
        this.transferemasked = transferemasked;
    }

    @Override
    public boolean isFin() {
        return fin;
    }

    @Override
    public boolean getTransfereMasked() {
        return transferemasked;
    }

    @Override
    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public ByteBuffer getPayloadData() {
        return payload;
    }

    @Override
    public void append(Framedata nextframe) throws InvalidFrameException {

    }
}
