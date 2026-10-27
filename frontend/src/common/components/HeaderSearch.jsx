import { useState } from 'react';

export default function HeaderSearch({ searchKeyword, onSearch }) {
    const [searchText, setSearchText] = useState(searchKeyword);
    const [prevKeyword, setPrevKeyword] = useState(searchKeyword);
    if (prevKeyword !== searchKeyword) {
        setPrevKeyword(searchKeyword);
        setSearchText(searchKeyword);
    }

    const handleSubmit = (event) => {
        event.preventDefault();
        onSearch?.(searchText.trim());
    };

    return (
        <form className="header-search" onSubmit={handleSubmit}>
            <input
                type="search"
                value={searchText}
                onChange={(event) => setSearchText(event.target.value)}
                placeholder="브랜드나 메뉴명을 검색해보세요 (예: 버거킹, 치킨)"
            />
            <button type="submit">검색</button>
        </form>
    );
}