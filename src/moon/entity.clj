(ns moon.entity)

(defsystem ->v "Create component value. Default returns v.")
(defmethod ->v :default [[_ v]] v)

(defsystem create [_ eid])
(defmethod create :default [_ eid])

(defsystem destroy [_ eid])
(defmethod destroy :default [_ eid])

(defsystem tick [_ eid])
(defmethod tick :default [_ eid])

(defsystem render-below [_ entity])
(defmethod render-below :default [_ entity])

(defsystem render [_ entity])
(defmethod render :default [_ entity])

(defsystem render-above [_ entity])
(defmethod render-above :default [_ entity])

(defsystem render-info [_ entity])
(defmethod render-info :default [_ entity])

(def render-systems [render-below render render-above render-info])

(declare stat
         modified-value)

(defn mana-value [entity]
  (if-let [mana (stat entity :stats/mana)]
    (mana 0)
    0))

(defn not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-value entity))))

;; State

(defsystem enter)
(defmethod enter :default [_])

(defsystem exit)
(defmethod exit :default [_])

(defsystem player-enter)
(defmethod player-enter :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem clicked-inventory-cell [_ cell])
(defmethod clicked-inventory-cell :default [_ cell])

(defsystem clicked-skillmenu-skill [_ skill])
(defmethod clicked-skillmenu-skill :default [_ skill])

(defn state-k [entity]
  (-> entity :entity/fsm :state))

(defn state-obj [entity]
  (let [k (state-k entity)]
    [k (k entity)]))
