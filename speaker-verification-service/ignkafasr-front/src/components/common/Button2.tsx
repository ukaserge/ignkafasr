import {CSSProperties, ReactNode, Ref} from "react";
import Link from "next/link";
import styled from "styled-components";

import { rem } from "@/helpers";

const buttonSizes = ["small", "medium"] as const;
type ButtonSize = typeof buttonSizes[number];

type ButtonProps = {
    /** @default '100%'' */
    width?: number | string;
    children: ReactNode;
    onClick?: (e) => void;
    href?: string;
    style?: CSSProperties;
    /** @default 'medium' */
    size?: ButtonSize;
};

export function Button2({ href, ...props }: ButtonProps) {
    if (href) {
        return (
            <button>
                <Wrapper {...props} />
            </button>
        );
    }

    return <Wrapper {...props} />;
}

const Wrapper = styled.a<{ width?: number | string; size?: ButtonSize }>`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;

  width: ${(props) => rem(props.width) ?? "100%"};
  height: 6.4rem;
  border-radius: 0.8rem;

  color: ${({ theme }) => theme.colors.gray1};
  font-size: 2.8rem;
  font-weight: 700;

  cursor: pointer;
  user-select: none;

  background-color: ${({ theme }) => theme.colors.gray7};
  transition: all 0.2s;
  &: hover {
    background-color: #ddd;
    color: ${({ theme }) => theme.colors.black};
  }

  ${(props) =>
    props.size === "small" &&
    `
    width: fit-content;
    height: fit-content;
    padding: 1.8rem 2.8rem;

    font-size: 2.2rem;
  `}
`;
