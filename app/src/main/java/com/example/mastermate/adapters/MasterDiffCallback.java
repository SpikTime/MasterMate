package com.example.mastermate.adapters;

import androidx.recyclerview.widget.DiffUtil;
import com.example.mastermate.models.Master;
import java.util.List;

public class MasterDiffCallback extends DiffUtil.Callback {
    private final List<Master> oldList;
    private final List<Master> newList;

    public MasterDiffCallback(List<Master> oldList, List<Master> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
