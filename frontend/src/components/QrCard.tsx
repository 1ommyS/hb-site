import { useEffect, useState } from "react";
import type { ReactElement } from "react";
import QRCode from "qrcode";

interface Props {
  text: string;
}

export function QrCard({ text }: Props): ReactElement {
  const [src, setSrc] = useState<string | null>(null);
  useEffect(() => {
    QRCode.toDataURL(text, {
      width: 240,
      margin: 1,
      color: { dark: "#0a0a0f", light: "#ffffff" },
    })
      .then(setSrc)
      .catch(() => setSrc(null));
  }, [text]);
  return (
    <div className="self-center rounded-lg bg-white p-3 shadow-[0_18px_40px_rgba(0,0,0,0.32)]">
      {src ? (
        <img src={src} alt="QR-код для подключения" className="h-48 w-48 sm:h-56 sm:w-56" />
      ) : (
        <div className="grid h-48 w-48 place-items-center text-xs text-zinc-500 sm:h-56 sm:w-56">
          QR…
        </div>
      )}
    </div>
  );
}
