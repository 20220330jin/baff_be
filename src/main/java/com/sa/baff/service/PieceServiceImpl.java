package com.sa.baff.service;

import com.sa.baff.domain.BattleRoom;
import com.sa.baff.domain.Piece;
import com.sa.baff.domain.PieceTransaction;
import com.sa.baff.domain.UserB;
import com.sa.baff.model.dto.PieceDto;
import com.sa.baff.repository.PieceRepository;
import com.sa.baff.repository.PieceTransactionRepository;
import com.sa.baff.repository.UserRepository;
import com.sa.baff.util.PieceTransactionType;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PieceServiceImpl implements PieceService {

    private final PieceRepository pieceRepository;
    private final PieceTransactionRepository pieceTransactionRepository;
    private final UserRepository userRepository;

    @Override
    public PieceDto.balanceResponse getBalance(String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N')
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Piece piece = getOrCreatePiece(user);
        return PieceDto.balanceResponse.builder()
                .balance(piece.getBalance())
                .build();
    }

    @Override
    public void deposit(String socialId, Long amount) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N')
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Piece piece = getOrCreatePiece(user);
        piece.addBalance(amount);
        pieceRepository.save(piece);

        PieceTransaction tx = PieceTransaction.builder()
                .user(user)
                .amount(amount)
                .type(PieceTransactionType.DEPOSIT)
                .build();
        pieceTransactionRepository.save(tx);
    }

    @Override
    public void deductForBet(UserB user, Long amount, BattleRoom battleRoom) {
        Piece piece = getOrCreatePiece(user);
        piece.deductBalance(amount);
        pieceRepository.save(piece);

        PieceTransaction tx = PieceTransaction.builder()
                .user(user)
                .amount(amount)
                .type(PieceTransactionType.BET_DEDUCT)
                .battleRoom(battleRoom)
                .build();
        pieceTransactionRepository.save(tx);
    }

    @Override
    public void grantBetWin(UserB winner, Long amount, BattleRoom battleRoom) {
        Piece piece = getOrCreatePiece(winner);
        piece.addBalance(amount);
        pieceRepository.save(piece);

        PieceTransaction tx = PieceTransaction.builder()
                .user(winner)
                .amount(amount)
                .type(PieceTransactionType.BET_WIN)
                .battleRoom(battleRoom)
                .build();
        pieceTransactionRepository.save(tx);
    }

    @Override
    public void refundBet(UserB user, Long amount, BattleRoom battleRoom) {
        Piece piece = getOrCreatePiece(user);
        piece.addBalance(amount);
        pieceRepository.save(piece);

        PieceTransaction tx = PieceTransaction.builder()
                .user(user)
                .amount(amount)
                .type(PieceTransactionType.BET_REFUND)
                .battleRoom(battleRoom)
                .build();
        pieceTransactionRepository.save(tx);
    }

    @Override
    public boolean hasEnoughBalance(String socialId, Long amount) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N')
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Piece piece = getOrCreatePiece(user);
        return piece.getBalance() >= amount;
    }

    @Override
    public PieceDto.transactionHistoryResponse getTransactionHistory(String socialId) {
        UserB user = userRepository.findUserIdBySocialIdAndDelYn(socialId, 'N')
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Piece piece = getOrCreatePiece(user);
        List<PieceTransaction> transactions = pieceTransactionRepository.findAllByUserOrderByRegDateTimeDesc(user);

        List<PieceDto.transactionInfo> txInfos = transactions.stream()
                .map(tx -> PieceDto.transactionInfo.builder()
                        .amount(tx.getAmount())
                        .type(tx.getType())
                        .description(getDescription(tx))
                        .createdAt(tx.getRegDateTime())
                        .build())
                .collect(Collectors.toList());

        return PieceDto.transactionHistoryResponse.builder()
                .balance(piece.getBalance())
                .transactions(txInfos)
                .build();
    }

    private Piece getOrCreatePiece(UserB user) {
        return pieceRepository.findByUser(user)
                .orElseGet(() -> {
                    Piece newPiece = new Piece(user);
                    return pieceRepository.save(newPiece);
                });
    }

    private String getDescription(PieceTransaction tx) {
        String roomName = tx.getBattleRoom() != null ? tx.getBattleRoom().getName() : "";
        return switch (tx.getType()) {
            case DEPOSIT -> "조각 지급";
            case BET_DEDUCT -> "배틀 내기 참여 - " + roomName;
            case BET_WIN -> "배틀 승리 보상 - " + roomName;
            case BET_REFUND -> "배틀 내기 환불 - " + roomName;
        };
    }
}
