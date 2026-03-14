package kwh.PublicCookedFood.board.service.query;

import java.util.Objects;
import java.util.Optional;

public interface BoardListQueryNotice {

    static BoardListQueryNotice none() {
        return None.INSTANCE;
    }

    static Message message(String value) {
        return new Message(value);
    }

    boolean present();

    Optional<String> text();

    final class None implements BoardListQueryNotice {
        private static final None INSTANCE = new None();

        private None() {
        }

        @Override
        public boolean present() {
            return false;
        }

        @Override
        public Optional<String> text() {
            return Optional.empty();
        }
    }

    final class Message implements BoardListQueryNotice {

        private final String value;

        public Message(String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("board list notice must not be blank");
            }
            this.value = value.trim();
        }

        @Override
        public boolean present() {
            return true;
        }

        @Override
        public Optional<String> text() {
            return Optional.of(value);
        }

        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Message)) {
                return false;
            }
            Message that = (Message) other;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}
