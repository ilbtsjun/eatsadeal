import React from 'react';
import './EventCardList.css';

export default function EventCardList() {
    // 임시 데이터 (나중에 백엔드 API에서 받아올 데이터)
    const dummyEvents = [
        { id: 1, brand: '버거킹', title: '와퍼 주니어 1+1 이벤트', discount: '50%', dDay: 'D-2', image: '🍔' },
        { id: 2, brand: 'BBQ', title: '황금올리브치킨 4,000원 할인', discount: '20%', dDay: 'D-Today', image: '🍗' },
        { id: 3, brand: '도미노피자', title: '방문포장 40% 특가 할인', discount: '40%', dDay: 'D-5', image: '🍕' },
        { id: 4, brand: '스타벅스', title: '오후 2시 이후 제조음료 50%', discount: '50%', dDay: 'D-1', image: '☕' },
    ];

    return (
        <div className="card-list-container">
            <div className="card-grid">
                {dummyEvents.map((item) => (
                    <div key={item.id} className="event-card">
                        <div className="card-image-box">
                            <span className="card-emoji">{item.image}</span>
                            <span className="card-dday">{item.dDay}</span>
                        </div>
                        <div className="card-info">
                            <span className="card-brand">{item.brand}</span>
                            <h3 className="card-title">{item.title}</h3>
                            <div className="card-footer">
                                <span className="card-discount">{item.discount}</span>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}