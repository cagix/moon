(ns cdq.ctx
  (:require [cdq.utils :as utils]
            [clojure.string :as str]))

(defn- valid-tx? [transaction]
  (vector? transaction))

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defn- handle-tx! [tx ctx]
  (assert (valid-tx? tx) (pr-str tx))
  (try
   (do! tx ctx)
   (catch Throwable t
     (throw (ex-info "Error handling transaction" {:transaction tx} t)))))

(defn handle-txs!
  "Handles transactions and returns a flat list of all transactions handled, including nested."
  [ctx transactions]
  (loop [ctx ctx
         txs transactions
         handled []]
    (if (seq txs)
      (let [tx (first txs)]
        (if tx
          (let [new-txs (handle-tx! tx ctx)]
              (recur ctx
                     (concat (or new-txs []) (rest txs))
                     (conj handled tx)))
          (recur ctx (rest txs) handled)))
      handled)))

(def ^:private k->colors {:property/pretty-name "PRETTY_NAME"
                          :entity/modifiers "CYAN"
                          :maxrange "LIGHT_GRAY"
                          :creature/level "GRAY"
                          :projectile/piercing? "LIME"
                          :skill/action-time-modifier-key "VIOLET"
                          :skill/action-time "GOLD"
                          :skill/cooldown "SKY"
                          :skill/cost "CYAN"
                          :entity/delete-after-duration "LIGHT_GRAY"
                          :entity/faction "SLATE"
                          :entity/fsm "YELLOW"
                          :entity/species "LIGHT_GRAY"
                          :entity/temp-modifier "LIGHT_GRAY"})

(def ^:private k-order [:property/pretty-name
                        :skill/action-time-modifier-key
                        :skill/action-time
                        :skill/cooldown
                        :skill/cost
                        :skill/effects
                        :entity/species
                        :creature/level
                        :creature/stats
                        :entity/delete-after-duration
                        :projectile/piercing?
                        :entity/projectile-collision
                        :maxrange
                        :entity-effects])

(defn- remove-newlines [s]
  (let [new-s (-> s
                  (str/replace "\n\n" "\n")
                  (str/replace #"^\n" "")
                  str/trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

(defmulti info-segment (fn [[k] _ctx] k))

(defmethod info-segment :default [_ _ctx])

(defn info-text
  "Creates a formatted informational text representation of components."
  [ctx components]
  (->> components
       (utils/sort-by-k-order k-order)
       (keep (fn [{k 0 v 1 :as component}]
               (str (let [s (try (info-segment component ctx)
                                 (catch Throwable t
                                   ; fails for
                                   ; effects/spawn
                                   ; end entity/hp
                                   ; as already 'built' yet 'hp' not
                                   ; built from db yet ...
                                   (pr-str component)
                                   #_(throw (ex-info "info system failed"
                                                     {:component component}
                                                     t))))]
                      (if-let [color (k->colors k)]
                        (str "[" color "]" s "[]")
                        s))
                    (when (map? v)
                      (str "\n" (info-text ctx v))))))
       (str/join "\n")
       remove-newlines))
