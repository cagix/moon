(ns cdq.stats)

(defprotocol Stats
  (get-stat-value [_ stat-k])
  (add    [_ mods])
  (remove-mods [_ mods])
  (get-mana [_])
  (mana-val [_])
  (not-enough-mana? [_ skill])
  (pay-mana-cost [_ cost])
  (get-hitpoints [_])
  (damage [_ damage]
          [_ target damage])
  (effective-armor-save [_ target-stats]))
