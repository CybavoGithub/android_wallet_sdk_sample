/**
 * Copyright (c) 2019 CYBAVO, Inc.
 * https://www.cybavo.com
 *
 * All rights reserved.
 */

package com.cybavo.example.wallet.detail;


import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cybavo.example.wallet.R;
import com.cybavo.example.wallet.helper.CurrencyHelper;
import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.example.wallet.helper.ToolbarHelper;
import com.cybavo.example.wallet.main.MainViewModel;
import com.cybavo.example.wallet.pincode.InputPinCodeDialog;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.auth.PinSecret;
import com.cybavo.wallet.service.wallet.Currency;
import com.cybavo.wallet.service.wallet.Transaction;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.GetTransactionInfoResult;
import com.cybavo.wallet.service.wallet.results.ReplaceTransactionResult;

import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class TransactionDetailFragment extends Fragment
        implements InputPinCodeDialog.OnPinCodeInputListener {

    private static final String ARG_WALLET = "wallet";
    private static final String ARG_TRANSACTION = "transaction";

    private MainViewModel mViewModel;
    private DetailViewModel mDetailViewModel;

    protected Wallet mWallet;
    private Transaction mTransaction;

    private TextView mWithdraw;
    private TextView mDeposit;
    private TextView mCurrency;
    private TextView mFrom;
    private TextView mTo;
    private TextView mAmountLabel;
    private TextView mAmount;
    private TextView mFee;
    private TextView mTxid;
    private TextView mReplaceTxid;
    private TextView mError;
    private TextView mTime;
    private TextView mMemo;
    private TextView mDescription;
    private TextView mPending;
    private TextView mFailed;
    private TextView mDropped;
    private TextView mReplaced;
    private TextView mConfirmBlocks;
    private TextView mPlatformFee;
    private Button mExplorer;
    private Button mSpeedUp;
    private Button mCancel;


    private enum Action {
        None,
        IncreaseFee,
        Cancel,
    }
    private Action mAction = Action.None;

    public TransactionDetailFragment() {
        // Required empty public constructor
    }

    public static TransactionDetailFragment newInstance(Wallet wallet, Transaction transaction) {
        TransactionDetailFragment fragment = new TransactionDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLET, WalletParcelable.fromWallet(wallet));
        args.putParcelable(ARG_TRANSACTION, TransactionParcelable.fromTransaction(transaction));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWallet = WalletParcelable.toWallet(getArguments().getParcelable(ARG_WALLET));
            mTransaction = TransactionParcelable.toTransaction(getArguments().getParcelable(ARG_TRANSACTION));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_transaction_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ToolbarHelper.setupToolbar(view, R.id.appBar)
                .title(R.string.title_transaction_detail)
                .onBack(v -> getFragmentManager().popBackStack())
                .done();

        mCurrency = view.findViewById(R.id.currency);
        mCurrency.setText(mWallet.currencySymbol);
        mCurrency.setCompoundDrawablesWithIntrinsicBounds(
                CurrencyHelper.getCoinIconResource(getContext(), mWallet.currencySymbol), 0, 0, 0);

        mWithdraw = view.findViewById(R.id.withdraw);
        mDeposit = view.findViewById(R.id.deposit);
        if (mTransaction.direction == Transaction.Direction.IN) {
            mDeposit.setVisibility(View.VISIBLE);
            mWithdraw.setVisibility(View.GONE);
        } else {
            mDeposit.setVisibility(View.GONE);
            mWithdraw.setVisibility(View.VISIBLE);
        }

        mFrom = view.findViewById(R.id.from);
        mFrom.setText(mTransaction.fromAddress);
        mTo = view.findViewById(R.id.to);
        mTo.setText(mTransaction.toAddress);
        mAmountLabel = view.findViewById(R.id.amountLabel);
        mAmount = view.findViewById(R.id.amount);
        mFee = view.findViewById(R.id.fee);
        mFee.setText(mTransaction.transactionFee);
        mTxid = view.findViewById(R.id.txid);
        if (!TextUtils.isEmpty(mTransaction.txid)) {
            mTxid.setVisibility(View.VISIBLE);
            mTxid.setText(mTransaction.txid);
        }
        mError = view.findViewById(R.id.error);
        if (!TextUtils.isEmpty(mTransaction.error)) {
            mError.setVisibility(View.VISIBLE);
            mError.setText(mTransaction.error);
        }

        mTime = view.findViewById(R.id.time);
        mTime.setText(DateFormat.format(
                DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMddHHmmss"),
                new Date(mTransaction.timestamp * 1000)));

        mMemo = view.findViewById(R.id.memo);
        mMemo.setText(TextUtils.isEmpty(mTransaction.memo) ? "-" : mTransaction.memo);

        mDescription = view.findViewById(R.id.description);
        mDescription.setText(TextUtils.isEmpty(mTransaction.description) ? "-" : mTransaction.description);

        mPending = view.findViewById(R.id.pending);
        mPending.setVisibility(mTransaction.pending ? View.VISIBLE : View.GONE);

        mFailed = view.findViewById(R.id.failed);
        mFailed.setVisibility(mTransaction.success ? View.GONE : View.VISIBLE);

        mDropped = view.findViewById(R.id.dropped);
        mDropped.setVisibility(mTransaction.dropped ? View.VISIBLE : View.GONE);
        if (mTransaction.dropped) {
            mTxid.setPaintFlags(mTxid.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        mReplaced = view.findViewById(R.id.replaced);
        mReplaced.setVisibility(mTransaction.replaced ? View.VISIBLE : View.GONE);
        mReplaceTxid = view.findViewById(R.id.replace_txid);
        if (mTransaction.replaced) {
            mTxid.setPaintFlags(mTxid.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            mReplaceTxid.setVisibility(View.VISIBLE);
            mReplaceTxid.setText(mTransaction.replaceTxid);
        } else {
            mReplaceTxid.setVisibility(View.GONE);
        }

        mConfirmBlocks = view.findViewById(R.id.confirmBlocks);

        mPlatformFee = view.findViewById(R.id.platformFee);
        mPlatformFee.setVisibility(mTransaction.platformFee ? View.VISIBLE : View.GONE);

        mExplorer = view.findViewById(R.id.explorer);
        final String uri = CurrencyHelper.getBlockExplorerUri(mWallet.currency, mWallet.tokenAddress, mTransaction.txid);
        if (uri != null) {
            mExplorer.setOnClickListener(v -> {
                browse(uri);
            });
        } else {
            mExplorer.setVisibility(View.GONE);
        }

        mSpeedUp = view.findViewById(R.id.speedUp);
        mSpeedUp.setVisibility(mTransaction.replaceable ? View.VISIBLE : View.GONE);
        mSpeedUp.setOnClickListener(v -> replaceTransactionFee());

        mCancel = view.findViewById(R.id.cancel);
        mCancel.setVisibility(mTransaction.replaceable ? View.VISIBLE : View.GONE);
        mCancel.setOnClickListener(v -> cancelTransaction());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = ViewModelProviders.of(getParentFragment(),
                new MainViewModel.Factory(getActivity().getApplication()))
                .get(MainViewModel.class);

        mViewModel.getCurrencies().observe(this, currencies -> {
            final Currency c = CurrencyHelper.findCurrency(currencies, mWallet);
            if (c != null) {
                mCurrency.setText(c.displayName);
                if(CurrencyHelper.isFungibleToken(c)){
                    mAmountLabel.setText(R.string.label_token_id);
                    mAmount.setText(mTransaction.amount);
                }else{
                    mAmountLabel.setText(R.string.label_amount);
                    mAmount.setText(getString(R.string.template_amount, mTransaction.amount, mWallet.currencySymbol));
                }
            } else {
                mCurrency.setText(mWallet.currencySymbol);
                mAmount.setText(getString(R.string.template_amount, mTransaction.amount, mWallet.currencySymbol));
            }
        });

        mDetailViewModel = ViewModelProviders.of(getParentFragment(),
                new DetailViewModel.Factory(getActivity().getApplication(), mWallet))
                .get(DetailViewModel.class);

        fetchTransactionInfo();
    }

    private void setInProgress(boolean inProgress) {
        mSpeedUp.setEnabled(!inProgress);
        mCancel.setEnabled(!inProgress);
    }

    private void fetchTransactionInfo() {
        if (mTransaction.txid.isEmpty() || mTransaction.dropped) {
            return;
        }

        Wallets.getInstance().getTransactionInfo(mWallet.currency, mTransaction.txid, new Callback<GetTransactionInfoResult>() {
            @Override
            public void onError(Throwable error) {
                Helpers.showToast(getContext(), "getTransactionInfo failed: " + error.getMessage());
            }

            @Override
            public void onResult(GetTransactionInfoResult result) {
                mConfirmBlocks.setVisibility(View.VISIBLE);
                mConfirmBlocks.setText(getString(R.string.label_confirm_blocks, result.confirmBlocks));

                mSpeedUp.setVisibility(result.replaceable ? View.VISIBLE : View.GONE);
                mCancel.setVisibility(result.replaceable ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void browse(String uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void replaceTransactionFee() {
        mAction = Action.IncreaseFee;
        InputPinCodeDialog dialog = InputPinCodeDialog.newInstance();
        dialog.show(getChildFragmentManager(), "pinCode");
    }


    private void cancelTransaction() {
        mAction = Action.Cancel;
        InputPinCodeDialog dialog = InputPinCodeDialog.newInstance();
        dialog.show(getChildFragmentManager(), "pinCode");
    }

    private final static String LARGER_TX_FEE = "0.0000003";
    // must be at least 10% higher than original transaction, let's just hard-coded a value for now

    @Override
    public void onPinCodeInput(PinSecret pinSecret) {
        if (mAction == Action.IncreaseFee) {

            Wallets.getInstance().increaseTransactionFee(mWallet.walletId, mTransaction.txid, LARGER_TX_FEE, pinSecret, new Callback<ReplaceTransactionResult>() {
                @Override
                public void onError(Throwable error) {
                    Helpers.showToast(getContext(), "replaceTransactionFee failed: " + error.getMessage());
                }

                @Override
                public void onResult(ReplaceTransactionResult replaceTransactionResult) {
                    mDetailViewModel.refreshHistory();
                    getFragmentManager().popBackStack();
                }
            });
        } else if (mAction == Action.Cancel) {
            Wallets.getInstance().cancelTransaction(mWallet.walletId, mTransaction.txid, LARGER_TX_FEE, pinSecret, new Callback<ReplaceTransactionResult>() {
                @Override
                public void onError(Throwable error) {
                    Helpers.showToast(getContext(), "cancelTransaction failed: " + error.getMessage());
                }

                @Override
                public void onResult(ReplaceTransactionResult replaceTransactionResult) {
                    mDetailViewModel.refreshHistory();
                    getFragmentManager().popBackStack();
                }
            });
        }
    }

    @Override
    public void onForgotPinCode() {

    }
}
