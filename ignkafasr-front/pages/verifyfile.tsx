import dynamic from "next/dynamic";

const MainContent = dynamic(() => import("../src/components/verifyfile"), {
    ssr: false,
});

export default function MainPage() {
    return <MainContent />;
}
