import dynamic from "next/dynamic";

const MainContent = dynamic(() => import("../src/components/registercontent"), {
    ssr: false,
});

export default function MainPage() {
    return <MainContent />;
}
