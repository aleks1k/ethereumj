package org.ethereum.net.p2p;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.ethereum.net.p2p.P2pMessage;
import org.ethereum.net.p2p.Peer;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.spongycastle.util.encoders.Hex;

/**
 * Wrapper around an Ethereum Peers message on the network
 * 
 * @see org.ethereum.net.p2p.P2pMessageCodes#PEERS
 */
public class PeersMessage extends P2pMessage {

	private boolean parsed = false;

	private Set<Peer> peers;

	public PeersMessage(byte[] payload) {
		super(payload);
	}

	public PeersMessage(Set<Peer> peers) {
		this.peers = peers;
		parsed = true;
	}

	private void parse() {
		RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

		peers = new LinkedHashSet<>();
		for (int i = 1; i < paramsList.size(); ++i) {
			RLPList peerParams = (RLPList) paramsList.get(i);
			byte[] ipBytes = peerParams.get(0).getRLPData();
			byte[] portBytes = peerParams.get(1).getRLPData();
			byte[] peerIdRaw = peerParams.get(2).getRLPData();

			try {
				int peerPort = ByteUtil.byteArrayToInt(portBytes);
				InetAddress address = InetAddress.getByAddress(ipBytes);

                String peerId = peerIdRaw == null ? "" :  Hex.toHexString(peerIdRaw);
                Peer peer = new Peer(address, peerPort, peerId);
				peers.add(peer);
			} catch (UnknownHostException e) {
				throw new RuntimeException("Malformed ip", e);
			}
		}
		this.parsed = true;
	}

	private void encode() {
		byte[][] encodedByteArrays = new byte[this.peers.size() + 1][];
		encodedByteArrays[0] = RLP.encodeByte(this.getCommand().asByte());
		List<Peer> peerList = new ArrayList<>(this.peers);
		for (int i = 0; i < peerList.size(); i++) {
			encodedByteArrays[i + 1] = peerList.get(i).getEncoded();
		}
		this.encoded = RLP.encodeList(encodedByteArrays);
	}

	@Override
	public byte[] getEncoded() {
		if (encoded == null) encode();
		return encoded;
	}

	public Set<Peer> getPeers() {
		if (!parsed) this.parse();
		return peers;
	}

    @Override
    public P2pMessageCodes getCommand(){
        return P2pMessageCodes.PEERS;
    }

	@Override
	public Class<?> getAnswerMessage() {
		return null;
	}

	public String toString() {
		if (!parsed) this.parse();

		StringBuffer sb = new StringBuffer();
		for (Peer peerData : peers) {
			sb.append("\n       ").append(peerData);
		}
		return "[" + this.getCommand().name() + sb.toString() + "]";
	}
}