import type { ButtonHTMLAttributes } from "react";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary";
};

export function Button({
  variant = "primary",
  className = "",
  ...props
}: ButtonProps) {
  const variantClass =
    variant === "primary" ? "primary-button" : "secondary-button";

  return (
    <button
      {...props}
      className={`${variantClass} ${className}`.trim()}
    />
  );
}
