(ns cdq.creature)

(defprotocol Skills
  (skill-usable-state [_ skill effect-ctx]))
