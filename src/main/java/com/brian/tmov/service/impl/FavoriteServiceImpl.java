package com.brian.tmov.service.impl;

import com.brian.tmov.dao.entity.FavoriteEntity;
import com.brian.tmov.dao.entity.MemberEntity;
import com.brian.tmov.dao.repository.FavoriteRepository;
import com.brian.tmov.dao.repository.MemberRepository;
import com.brian.tmov.dto.request.FavoriteRequest;
import com.brian.tmov.enums.MediaType;
import com.brian.tmov.service.FavoriteService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Override
    @Transactional
    public void addFavorite(String email, FavoriteRequest request) {
        if (favoriteRepository.existsByMemberEmailAndTmdbIdAndMediaType(
                email, request.tmdbId(), request.mediaType())) {
            throw new IllegalArgumentException("已在收藏清單中");
        }

        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("會員不存在"));

        FavoriteEntity favorite = new FavoriteEntity(member, request.tmdbId(), request.mediaType());
        favoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(String email, FavoriteRequest request) {
        favoriteRepository.deleteByMemberEmailAndTmdbIdAndMediaType(
                email, request.tmdbId(), request.mediaType()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(String email, Long tmdbId, String mediaTypeStr) {
        try {
            MediaType type = MediaType.valueOf(mediaTypeStr.toLowerCase());
            return favoriteRepository.existsByMemberEmailAndTmdbIdAndMediaType(email, tmdbId, type);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
