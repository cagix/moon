(ns cdq.context.player-message)

(defn create [[_ {:keys [duration-seconds]}] _context]
  (atom {:duration-seconds duration-seconds}))
