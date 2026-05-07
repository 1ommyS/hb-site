import { useEffect, useState } from "react";
import QRCode from "qrcode";

interface Props {
  text: string;
}

export function QrCard({ text }: Props): JSX.Element {
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
    <div className="rounded-2xl bg-white p-3 self-center">
      {src ? (
        <img src={src} alt="QR-код для подключения" className="w-56 h-56" />
      ) : (
        <div className="w-56 h-56 grid place-items-center text-zinc-500 text-xs">
          QR…
        </div>
      )}
    </div>
  );
}
