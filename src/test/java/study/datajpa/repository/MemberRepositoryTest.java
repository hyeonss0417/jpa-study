package study.datajpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.entity.Team;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(false)
class MemberRepositoryTest {
    @Autowired MemberRepository memberRepository;
    @Autowired TeamRepository teamRepository;
    @PersistenceContext EntityManager em;

    @Test
    public void testMember() {
        Member member = new Member("memberA");
        Member savedMember = memberRepository.save(member);

        Optional<Member> findMember = memberRepository.findById(savedMember.getId());

        assertTrue(findMember.isPresent());
        assertEquals(findMember.get().getId(), member.getId());
        assertEquals(findMember.get().getUsername(), member.getUsername());
    }

    @Test
    public void basicCRUD() {
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");
        memberRepository.save(member1);
        memberRepository.save(member2);

        Member findMember1 = memberRepository.findById(member1.getId()).get();
        Member findMember2 = memberRepository.findById(member2.getId()).get();
        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        long count = memberRepository.count();
        assertThat(count).isEqualTo(2);

        memberRepository.delete(member1);
        memberRepository.delete(member2);

        long count2 = memberRepository.count();
        assertThat(count2).isEqualTo(0);
    }

    @Test
    public void findByUsernameAndAgeGreaterThan() {
        Member m1 = new Member("test", 10, null);
        Member m2 = new Member("test", 20, null);
        memberRepository.save(m1);
        memberRepository.save(m2);

        List<Member> result = memberRepository.findByUsernameAndAgeGreaterThan("test", 15);

        assertThat(result.get(0)).isEqualTo(m2);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void findUser() {
        Member m1 = new Member("test", 10, null);
        Member m2 = new Member("test", 20, null);
        memberRepository.save(m1);
        memberRepository.save(m2);

        List<Member> result = memberRepository.findUser("test", 10);

        assertThat(result.get(0)).isEqualTo(m1);
    }

    @Test
    public void findUsernames() {
        Member m1 = new Member("test1", 10, null);
        Member m2 = new Member("test2", 20, null);
        memberRepository.save(m1);
        memberRepository.save(m2);

        List<String> result = memberRepository.findUsernames();

        assertThat(result).containsExactly("test1", "test2");
    }

    @Test
    public void findMemberDto() {
        Team team = new Team("teamA");
        teamRepository.save(team);
        Member m1 = new Member("test1", 10, team);
        memberRepository.save(m1);

        List<MemberDto> result = memberRepository.findMemberDtoList();

        assertThat(result.get(0)).isEqualTo(new MemberDto(m1.getId(), "test1", "teamA"));
    }

    @Test
    public void findPageBy() {
        memberRepository.save(new Member("member1", 10, null));
        memberRepository.save(new Member("member2", 10, null));
        memberRepository.save(new Member("member3", 10, null));
        memberRepository.save(new Member("member4", 10, null));

        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));
        Page<Member> page = memberRepository.findPageBy(pageRequest);

        List<Member> content = page.getContent();
        long totalElements = page.getTotalElements();
        assertThat(content).extracting("username").containsExactly("member4", "member3", "member2");
        assertThat(totalElements).isEqualTo(4);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    public void findSliceBy() {
        memberRepository.save(new Member("member1", 10, null));
        memberRepository.save(new Member("member2", 10, null));
        memberRepository.save(new Member("member3", 10, null));
        memberRepository.save(new Member("member4", 10, null));

        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));
        Slice<Member> slice = memberRepository.findSliceBy(pageRequest);

        List<Member> content = slice.getContent();
        assertThat(content).extracting("username").containsExactly("member4", "member3", "member2");
        assertThat(slice.getNumber()).isEqualTo(0);
        assertThat(slice.isFirst()).isTrue();
        assertThat(slice.hasNext()).isTrue();
    }

    @Test
    public void findPageByAge() {
        memberRepository.save(new Member("member1", 10, null));
        memberRepository.save(new Member("member2", 10, null));

        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "username"));
        Page<Member> page = memberRepository.findPageByAge(10, pageRequest);

        assertThat(page.getContent()).extracting("username").containsExactly("member2");
    }

    @Test
    public void bulkIncAge() {
        Member m1 = memberRepository.save(new Member("test", 10, null));
        Member m2 = memberRepository.save(new Member("test", 20, null));
        Member m3 = memberRepository.save(new Member("test", 30, null));

        int result = memberRepository.bulkIncAge();

        assertThat(result).isEqualTo(3);
        List<Member> members = memberRepository.findAll();
        assertThat(members.get(0).getAge()).isEqualTo(11);
        assertThat(members.get(1).getAge()).isEqualTo(21);
        assertThat(members.get(2).getAge()).isEqualTo(31);
    }

    @Test
    public void findMemberWithTeam() {
        memberRepository.save(new Member("member1", 10, new Team("teamA")));
        memberRepository.save(new Member("member2", 10, new Team("teamB")));
        em.flush();
        em.clear();

        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        List<Member> members = memberRepository.findMemberWithTeam();
        members.forEach(m -> m.getTeam().getName());
        statistics.setStatisticsEnabled(false);

        long fetchCount = statistics.getEntityFetchCount();
        assertThat(fetchCount).isEqualTo(0);
    }

    @Test
    public void findAll() {
        memberRepository.save(new Member("member1", 10, new Team("teamA")));
        memberRepository.save(new Member("member2", 10, new Team("teamB")));
        em.flush();
        em.clear();

        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        List<Member> members = memberRepository.findAll();
        members.forEach(m -> m.getTeam().getName());
        statistics.setStatisticsEnabled(false);

        long fetchCount = statistics.getEntityFetchCount();
        assertThat(fetchCount).isEqualTo(0);
    }

    @Test
    public void findMemberCustom() {
        memberRepository.saveAndFlush(new Member("test"));
        List<Member> result = memberRepository.findMemberCustom();
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void JpaEventBaseEntity() throws InterruptedException {
        Member m = memberRepository.save(new Member("member1")); // @PrePersist
        Thread.sleep(100);
        m.setUsername("member2");

        em.flush(); // @PreUpdate
        em.clear();

        Member findMember = memberRepository.findById(m.getId()).get();
        // Assert diff 100
        long diff = ChronoUnit.MILLIS.between(findMember.getCreatedAt(), findMember.getUpdatedAt());
        assertThat(diff).isGreaterThan(100);
        System.out.println("createdAt = " + findMember.getCreatedAt());
        System.out.println("updatedAt = " + findMember.getUpdatedAt());
        System.out.println("createdBy = " + findMember.getCreatedBy());
        System.out.println("updatedBy = " + findMember.getLastModifiedBy());
    }
}