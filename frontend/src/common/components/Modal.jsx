import { useEffect } from 'react';

export default function Modal({ onClose, locked = false, backdropClassName, children }) {
    useEffect(() => {
        if (locked)
            return undefined;
        const handleKeyDown = (event) => {
            if (event.key === 'Escape')
                onClose();
        };
        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [locked, onClose]);

    return (
        <div
            className={backdropClassName}
            role="presentation"
            onMouseDown={(event) => { if (!locked && event.target === event.currentTarget)
                onClose();
            }}
        >
            {children}
        </div>
    );
}