(ns cdq.entity)

(defmulti create (fn [[k] ctx]
                   k))
(defmethod create :default [[_ v] ctx]
  v)

(defmulti create! (fn [[k] eid ctx]
                    k))
(defmethod create! :default [_ eid ctx])

(defmulti destroy! (fn [[k] eid ctx]
                    k))
(defmethod destroy! :default [_ eid ctx])

(defmulti tick! (fn [[k] eid ctx]
                  k))
(defmethod tick! :default [_ eid ctx])

(defmulti  render-below! (fn [[k] entity ctx] k))
(defmethod render-below! :default [_ _entity ctx])

(defmulti  render-default! (fn [[k] entity ctx] k))
(defmethod render-default! :default [_ _entity ctx])

(defmulti  render-above! (fn [[k] entity ctx] k))
(defmethod render-above! :default [_ _entity ctx])

(defmulti  render-info! (fn [[k] entity ctx] k))
(defmethod render-info! :default [_ _entity ctx])

(defprotocol Entity
  (position [_])
  (in-range? [_ target maxrange])
  (overlaps? [_ other-entity])
  (rectangle [_])
  (id [_])
  (faction [_])
  (enemy [_])
  (state-k [_])
  (state-obj [_])
  (skill-usable-state [_ skill effect-ctx])
  (mod-add    [_ mods])
  (mod-remove [_ mods])
  (stat [_ k])
  (mana [_])
  (mana-val [_])
  (hitpoints [_])
  (pay-mana-cost [_ cost]))
