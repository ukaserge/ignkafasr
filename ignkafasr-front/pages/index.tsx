import styled from "styled-components";

import { Layout, Button } from "@/components/common";

export default function HomePage() {
  return (
    <Layout>
      <Title>Speaker Verification</Title>
      <Description>
       with Speechbrain, Whisper, Apache Ignite, Kafka
      </Description>
      <Row>
        <Button href="/upload" width={288}>
          Register Voice
        </Button>
        <Button href="/main" width={288}>
          Verify Voice
        </Button>
      </Row>
    </Layout>
  );
}

const Title = styled.h1`
  margin-top: 8rem;
  margin-bottom: 2.4rem;

  font-size: 3.2rem;
  font-weight: 700;
  color: ${({ theme }) => theme.colors.gray1};
`;

const Description = styled.p`
  margin-bottom: 4.8rem;

  text-align: center;

  font-size: 2.4rem;
  color: ${({ theme }) => theme.colors.gray4};
`;

const Row = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;

  width: 100%;
  margin-top: auto;
`;
