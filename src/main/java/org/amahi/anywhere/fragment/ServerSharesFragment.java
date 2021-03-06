/*
 * Copyright (c) 2014 Amahi
 *
 * This file is part of Amahi.
 *
 * Amahi is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Amahi is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Amahi. If not, see <http ://www.gnu.org/licenses/>.
 */

package org.amahi.anywhere.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.squareup.otto.Subscribe;

import org.amahi.anywhere.AmahiApplication;
import org.amahi.anywhere.R;
import org.amahi.anywhere.adapter.ServerSharesAdapter;
import org.amahi.anywhere.bus.BusProvider;
import org.amahi.anywhere.bus.ServerConnectionChangedEvent;
import org.amahi.anywhere.bus.ServerSharesLoadFailedEvent;
import org.amahi.anywhere.bus.ServerSharesLoadedEvent;
import org.amahi.anywhere.bus.ShareSelectedEvent;
import org.amahi.anywhere.server.client.ServerClient;
import org.amahi.anywhere.server.model.ServerShare;
import org.amahi.anywhere.util.ViewDirector;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Shares fragment. Shows shares list.
 */
public class ServerSharesFragment extends ListFragment
{
	private static final class State
	{
		private State() {
		}

		public static final String SHARES = "shares";
	}

	@Inject
	ServerClient serverClient;

	@Override
	public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
		return layoutInflater.inflate(R.layout.fragment_server_shares, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setUpInjections();

		setUpShares(savedInstanceState);
	}

	private void setUpInjections() {
		AmahiApplication.from(getActivity()).inject(this);
	}

	private void setUpShares(Bundle state) {
		setUpSharesAdapter();
		setUpSharesContent(state);
	}

	private void setUpSharesAdapter() {
		setListAdapter(new ServerSharesAdapter(getActivity()));
	}

	private void setUpSharesContent(Bundle state) {
		if (isSharesStateValid(state)) {
			setUpSharesState(state);
		} else {
			setUpSharesContent();
		}
	}

	private boolean isSharesStateValid(Bundle state) {
		return (state != null) && (state.containsKey(State.SHARES));
	}

	private void setUpSharesState(Bundle state) {
		List<ServerShare> shares = state.getParcelableArrayList(State.SHARES);

		setUpSharesContent(shares);

		showSharesContent();
	}

	private void setUpSharesContent(List<ServerShare> shares) {
		getSharesAdapter().replaceWith(shares);
	}

	private ServerSharesAdapter getSharesAdapter() {
		return (ServerSharesAdapter) getListAdapter();
	}

	private void showSharesContent() {
		ViewDirector.of(this, R.id.animator).show(R.id.content);
	}

	private void setUpSharesContent() {
		if (serverClient.isConnected()) {
			serverClient.getShares();
		}
	}

	@Subscribe
	public void onServerConnectionChanged(ServerConnectionChangedEvent event) {
		serverClient.getShares();
	}

	@Subscribe
	public void onSharesLoaded(ServerSharesLoadedEvent event) {
		setUpSharesContent(event.getServerShares());

		showSharesContent();
	}

	@Subscribe
	public void onSharesLoadFailed(ServerSharesLoadFailedEvent event) {
		showSharesError();
	}

	private void showSharesError() {
		ViewDirector.of(this, R.id.animator).show(R.id.error);
	}

	@Override
	public void onListItemClick(ListView sharesListView, View shareView, int sharePosition, long shareId) {
		super.onListItemClick(sharesListView, shareView, sharePosition, shareId);

		startShareOpening(getSharesAdapter().getItem(sharePosition));
	}

	private void startShareOpening(ServerShare share) {
		BusProvider.getBus().post(new ShareSelectedEvent(share));
	}

	@Override
	public void onResume() {
		super.onResume();

		BusProvider.getBus().register(this);
	}

	@Override
	public void onPause() {
		super.onPause();

		BusProvider.getBus().unregister(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		tearDownSharesState(outState);
	}

	private void tearDownSharesState(Bundle state) {
		if (areSharesLoaded()) {
			state.putParcelableArrayList(State.SHARES, new ArrayList<Parcelable>(getSharesAdapter().getItems()));
		}
	}

	private boolean areSharesLoaded() {
		return getSharesAdapter() != null;
	}
}
