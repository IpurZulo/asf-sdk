package com.asf.appcoins.sdk.core.microraidenj;

import com.asf.appcoins.sdk.core.web3.AsfWeb3jImpl;
import com.asf.microraidenj.MicroRaidenImpl;
import com.asf.microraidenj.eth.interfaces.GetChannelBlock;
import com.asf.microraidenj.eth.interfaces.TransactionSender;
import com.asf.microraidenj.exception.DepositTooHighException;
import com.asf.microraidenj.exception.TransactionFailedException;
import com.asf.microraidenj.type.Address;
import ethereumj.crypto.ECKey;
import java.math.BigInteger;
import java.util.logging.Logger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.http.HttpService;

public class Sample {

  public static void main(String[] args) throws TransactionFailedException {

    Web3j web3j =
        Web3jFactory.build(new HttpService("https://ropsten.infura.io/1YsvKO0VH5aBopMYJzcy"));
    AsfWeb3jImpl asfWeb3j = new AsfWeb3jImpl(web3j);

    Address channelManagerAddr = new Address("0x97a3e71e4d9cb19542574457939a247491152e81");
    Address tokenAddr = new Address("0xab949343E6C369C6B17C7ae302c1dEbD4B7B61c3");
    Logger log = Logger.getLogger(MicroRaidenImpl.class.getSimpleName());
    BigInteger maxDeposit = BigInteger.valueOf(10);
    TransactionSender transactionSender =
        new TransactionSenderImpl(asfWeb3j, () -> 50000000000L, new GetNonceImpl(asfWeb3j),
            new GasLimitImpl(web3j));

    GetChannelBlock getChannelBlock =
        createChannelTxHash -> new GetChannelBlockImpl(web3j, 3, 1500).get(createChannelTxHash);

    MicroRaidenImpl microRaiden =
        new MicroRaidenImpl(channelManagerAddr, tokenAddr, log, maxDeposit, transactionSender,
            getChannelBlock);

    // Put a private key
    ECKey senderECKey = ECKey.fromPrivate(new BigInteger("", 16));
    ECKey receiverEcKey = ECKey.fromPrivate(
        new BigInteger("dd615cb6205e116410272c5c885ec1fcc1728bac667704523cc79a694fd61227", 16));

    Address receiverAddress = Address.from(receiverEcKey.getAddress());

    BigInteger openBlockNumber = null;
    try {
      openBlockNumber =
          microRaiden.createChannel(senderECKey, receiverAddress, BigInteger.valueOf(1));

      microRaiden.topUpChannel(senderECKey, receiverAddress, maxDeposit, openBlockNumber);
    } catch (TransactionFailedException | DepositTooHighException e) {
      throw new RuntimeException(e);
    }

    BigInteger owedBalance = BigInteger.valueOf(1);

    byte[] balanceMsgHashSigned =
        microRaiden.getBalanceMsgHashSigned(Address.from(receiverEcKey.getAddress()),
            openBlockNumber, owedBalance, senderECKey);

    byte[] closingMsgHashSigned =
        microRaiden.getClosingMsgHashSigned(Address.from(senderECKey.getAddress()), openBlockNumber,
            owedBalance, receiverEcKey);

    String txHash =
        microRaiden.closeChannelCooperatively(senderECKey, Address.from(receiverEcKey.getAddress()),
            openBlockNumber, owedBalance, balanceMsgHashSigned, closingMsgHashSigned);
  }
}