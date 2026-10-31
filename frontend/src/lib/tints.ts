// Pastel card colours, cycled by position. Literal class strings so Tailwind picks them up.
const TINTS = [
  "bg-me-soft text-me-ink",
  "bg-you-soft text-you-ink",
  "bg-mint-soft text-mint-ink",
  "bg-peach-soft text-peach-ink",
  "bg-lemon-soft text-lemon-ink",
];

export const tint = (i: number) => TINTS[i % TINTS.length];
