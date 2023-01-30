import dynamic from "next/dynamic";

const MainContent = dynamic(() => import("../src/components/maincontent"), {
  ssr: false,
});

export default function MainPage() {
  return <MainContent />;
}
