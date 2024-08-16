(ns entity.stats
  (:require [clojure.string :as str]
            [clojure.math :as math]
            [gdx.graphics.color :as color]
            [utils.random :as random]
            [data.val-max :refer [val-max-ratio lower-than-max? set-to-max apply-val-max-modifier]]
            [core.component :refer [defcomponent]]
            [core.data :as data]
            [api.effect :as effect]
            [api.entity :as entity]
            [api.graphics :as g]
            [api.tx :refer [transact!]]
            [context.ui.config :refer (hpbar-height-px)]))

; TODO
; * default values / schema for creature stats.... (which required/optional ...)

; * bounds (action-speed not <=0 , not value '-1' e.g.)/schema/values/allowed operations
; * take max-movement-speed into account @ :stats/movement-speed

; * damage min/max applied doesn't make sense ... its half a damage difference
; make like D2 ...

; * :inc :mult -> use namespaced keyword

; * modifier / modifier-component
  ; or modifiers/ modifier
  ; and effects/effect
  ; effects/usable? .... plural ?

(defn- tx-update-modifiers [entity modifier f]
  [:tx.entity/update-in entity [:entity/stats :stats/modifiers modifier] f])

(defmethod transact! :tx/apply-modifier [[_ entity modifiers] ctx]
  (for [[modifier value] modifiers]
    (tx-update-modifiers entity modifier #(conj % value))))

(defmethod transact! :tx/reverse-modifier [[_ entity modifiers] ctx]
  (for [[modifier value] modifiers]
    (tx-update-modifiers entity modifier (fn [values]
                                           {:post [(= (count %) (dec (count values)))]}
                                           (remove #{value} values)))))

; TODO data.operations ???
; or each data/foo defines its operations - modifiers/effects .... !
; open-closed principle ... cannot extend datas/operations ...

; For now no green/red color for positive/negative numbers
; as :stats/damage-receive negative value would be red but actually a useful buff
(def ^:private positive-modifier-color "[CYAN]" #_"[LIME]")
(def ^:private negative-modifier-color "[CYAN]" #_"[SCARLET]")

(defn- +? [n]
  (case (math/signum n)
    (0.0 1.0) (str positive-modifier-color "+")
    -1.0 (str negative-modifier-color "")))

(defn- ->percent [v]
  (str (int (* 100 v)) "%"))

(defn- stat-k->pretty-name [stat-k]
  (str/capitalize (name stat)))

(defn info-text [[[stat-k operation] value]]
  (str (+? value)
       (case operation
         :inc (str value " ")
         :mult (str (->percent value) " ")
         [:val :inc] (str value " min ")
         [:max :inc] (str value " max ")
         [:val :mult] (str (->percent value) " min ")
         [:max :mult] (str (->percent value) " max "))
       (stat-k->pretty-name stat-k)
       "[]"))

(defmulti apply-operation (fn [operation _base-value _values]
                            (cond
                             (#{[:max :inc]
                                [:max :mult]
                                [:val :inc]
                                [:val :mult]} operation)
                             :op/val-max

                             :else
                             operation)))

; TODO use :op/inc / :op/mult ....?
; TODO @ data.val-max DRY
; TODO all modifiers always get reduce + 'd
(defmethod apply-operation :inc [_ base-value values]
  (reduce + base-value values))

(defmethod apply-operation :mult [_ base-value values]
  (* base-value (reduce + 1 values)))

(defmethod apply-operation :op/val-max  [operation base-value values]
  (apply-val-max-modifier base-value [operation values]))

(defn- op-order [[[_stat-k operation] _values]]
  (case operation
    :inc 0
    :mult 1
    [:val :inc] 0
    [:val :mult] 1
    [:max :inc] 0
    [:max :mult] 1))

(defn- ->effective-value [base-value stat-k stats]
  (->> stats
       :stats/modifiers
       (filter (fn [[[k _op] _values]] (= k stat-k)))
       (sort-by op-order)
       (reduce (fn [base-value [[_k operation] values]]
                 (apply-operation operation base-value values))
               base-value)))

(comment

 (= (->effective-value [5 10]
                       :stats/damage-deal
                       {:stats/modifiers {[:stats/damage-deal [:val :inc]] [30]
                                          [:fooz :barz] [ 1 2 3 ]}})
    [35 35])

 (= (->effective-value [5 10]
                       :stats/damage-deal
                       {:stats/modifiers {}})
    [5 10])


 (= (->effective-value :stats/hp
                       {:stats/modifiers {[:stats/hp [:max :inc]] [10 1]
                                          [:stats/hp [:max :mult]] [0.5]}
                        :stats/hp [100 100]})
    [100 166])

 (= (->effective-value :stats/movement-speed
                       {:stats/modifiers {[:stats/movement-speed :inc] [2]
                                          [:stats/movement-speed :mult] [0.1 0.2]}
                        :stats/movement-speed 3})
    6.5)

 )

(def ^:private operation-components-base
  {[:max :inc] {:type :component/modifier
                :widget :text-field
                :schema int?}
   [:max :mult] {:type :component/modifier
                 :widget :text-field
                 :schema number?}
   [:val :inc] {:type :component/modifier
                :widget :text-field
                :schema int?}
   [:val :mult] {:type :component/modifier
                 :widget :text-field
                 :schema number?}
   :inc {:type :component/modifier
         :widget :text-field
         :schema number?}
   ; TODO for strength its only int increase, for movement-speed different .... ??? how to manage this ?
   :mult {:type :component/modifier
          :widget :text-field
          :schema number?}})

(defn defstat [stat-k attr-data & {:keys [operations]}]
  (defcomponent stat-k attr-data)
  (doseq [op operations]
    (defcomponent [stat-k op] (get operation-components-base op)))
  stat-k)

(defstat :stats/hp data/pos-int-attr
  :operations [[:max :inc]
               [:max :mult]])

(defstat :stats/mana data/nat-int-attr
  :operations [[:max :inc]
               [:max :mult]])

(defstat :stats/movement-speed data/pos-attr
  :operations [:inc :mult])

(defstat :stats/strength data/nat-int-attr
  :operations [:inc])

(let [doc "action-time divided by this stat when a skill is being used.
          Default value 1.

          For example:
          attack/cast-speed 1.5 => (/ action-time 1.5) => 150% attackspeed."
      skill-speed-stat (assoc data/pos-attr :doc doc)
      operations [:inc]]
  (defstat :stats/cast-speed skill-speed-stat
    :operations operations)
  (defstat :stats/attack-speed skill-speed-stat
    :operations operations))

(defstat :stats/armor-save {:widget :text-field :schema number?}
  :operations [:inc])

(defstat :stats/armor-pierce {:widget :text-field :schema number?}
  :operations [:inc])

; TODO this is not a real stat ... ?
; only as modifiers ?
; don't add to :entity/stats component .....
(defstat :stats/damage-deal {}
  :operations [[:max :inc]
               [:max :mult]
               [:val :inc]
               [:val :mult]])

(defstat :stats/damage-receive {}
  :operations [[:max :inc]
               [:max :mult]
               [:val :inc]
               [:val :mult]])

(defcomponent :stats/modifiers
  (data/components
    (filter (fn [[stat op]]
              (#{:stats/damage-deal :stats/damage-receive} stat) )
            (map first (filter (fn [[k data]]
                                 (= (:type data) :component/modifier))
                               core.component/attributes)))))

(extend-type api.entity.Entity
  entity/Stats
  (stat [{:keys [entity/stats]} stat-k]
    (when-let [base-value (stat-k stats)]
      (->effective-value base-value stat-k stats))))

; TODO remove vector shaboink -> gdx.graphics.color/->color use.
(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
                (> ratio 0.75) :green
                (> ratio 0.5)  :darkgreen
                (> ratio 0.25) :yellow
                :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)


(comment

 (set! *print-level* nil)

 ; only lady/lord a&b dont have hp/mana/movement-speed ....
 ; => check if it can be optional
 ; => default-values move to stats ...

 (map :property/id
      (filter #(let [{:stats/keys [hp mana movement-speed]} (:entity/stats (:creature/entity %))

                     rslt (not (and hp mana movement-speed))
                     ]
                 (println [hp mana movement-speed])
                 (println rslt)
                 rslt
                 )
              (api.context/all-properties @app.state/current-context :properties/creature)))

 )



(def ^:private stats-keywords
  ; hp/mana/movement-speed given for all entities ?
  ; but maybe would be good to have 'required' key ....
  ; also default values defined with the stat itself ?
  ; armor-pierce move out of here ...
  [:stats/hp
   :stats/mana
   :stats/movement-speed
   :stats/strength  ; default value 0
   :stats/cast-speed ; has default value 1 @ entity.stateactive-skill
   :stats/attack-speed ; has default value 1 @ entity.stateactive-skill
   :stats/armor-save ; default-value 0
   :stats/armor-pierce ; default-value 0
   ; TODO damage deal/receive ?
   ])

(defcomponent :entity/stats (data/components
                              (filter #(and (keyword? %)
                                            (= (name :stats) (namespace %))
                                            (not (#{:stats/damage-deal :stats/damage-receive} %)))
                                      (keys core.component/attributes)))
  (entity/create-component [[_ stats] _components _ctx]
    (-> stats
        (update :stats/hp (fn [hp] [hp hp]))
        (update :stats/mana (fn [mana] [mana mana]))
        (update :stats/modifiers (fn [mods-values]
                                   (zipmap (keys mods-values)
                                           (map vector (vals mods-values)))))))

  ; TODO proper texts...
  ; HP color based on ratio like hp bar samey (take same color definitions etc.)
  ; mana color same in the whole app
  ; red positive/green negative
  (entity/info-text [[_ {:keys [stats/modifiers] :as stats}] _ctx]
    (str (str/join "\n"
                   (for [stat-k stats-keywords
                         :let [base-value (stat-k stats)]
                         :when base-value]
                     (str (stat-k->pretty-name stat-k) ": " (->effective-value base-value stat-k stats))))
         (when (seq modifiers)
           (str "\n"
                (str/join "\n"
                          (for [[modifier values] modifiers]
                            (info-text [modifier (reduce + values)])))))))

  (entity/render-info [_
                       {{:keys [width half-width half-height]} :entity/body
                        :keys [entity/mouseover?] :as entity*}
                       g
                       _ctx]
    (let [ratio (val-max-ratio (entity/stat entity* :stats/hp))
          [x y] (entity/position entity*)]
      (when (or (< ratio 1) mouseover?)
        (let [x (- x half-width)
              y (+ y half-height)
              height (g/pixels->world-units g hpbar-height-px)
              border (g/pixels->world-units g borders-px)]
          (g/draw-filled-rectangle g x y width height color/black)
          (g/draw-filled-rectangle g
                                   (+ x border)
                                   (+ y border)
                                   (- (* width ratio) (* 2 border))
                                   (- height (* 2 border))
                                   (hpbar-color ratio)))))))

(defmethod transact! :tx.entity.stats/pay-mana-cost [[_ entity cost] _ctx]
  (let [mana-val ((entity/stat @entity :stats/mana) 0)]
    (assert (<= cost mana-val))
    [[:tx.entity/assoc-in entity [:entity/stats :stats/mana 0] (- mana-val cost)]]))

(comment
 (let [mana-val 4
       entity (atom (entity/map->Entity {:entity/stats {:stats/mana [mana-val 10]}}))
       mana-cost 3
       resulting-mana (- mana-val mana-cost)]
   (= (transact! [:tx.entity.stats/pay-mana-cost entity mana-cost] nil)
      [[:tx.entity/assoc-in entity [:entity/stats :stats/mana 0] resulting-mana]]))
 )

(defmacro def-set-to-max-effect [stat]
  `(let [component# ~(keyword "effect" (str (name (namespace stat)) "-" (name stat) "-set-to-max"))]
     (defcomponent component# {:widget :label
                               :schema [:= true]
                               :default-value true}
       (effect/text ~'[_ _effect-ctx]
         ~(str "Sets " (name stat) " to max."))

       (effect/usable? ~'[_ _effect-ctx] true)

       (effect/useful? ~'[_ {:keys [effect/source]} _ctx]
         (lower-than-max? (~stat (:entity/stats @~'source))))

       (transact! ~'[_ {:keys [effect/source]}]
         [[:tx.entity/assoc-in ~'source [:entity/stats ~stat] (set-to-max (~stat (:entity/stats @~'source)))]]))))

(def-set-to-max-effect :stats/hp)
(def-set-to-max-effect :stats/mana)

#_(defcomponent :effect/set-to-max {:widget :label
                                    :schema [:= true]
                                    :default-value true}
  (effect/text [[_ stat] _effect-ctx]
    (str "Sets " (name stat) " to max."))

  (effect/usable? [_ {:keys [effect/source]}]
    source)

  (effect/useful? [[_ stat] {:keys [effect/source]} _ctx]
    (lower-than-max? (stat @source)))

  (effect/txs [[_ stat] {:keys [effect/source]}]
    [[:tx.entity/assoc source stat (set-to-max (stat @source))]]))

; this as macro ... ? component which sets the value of another component ??
#_(defcomponent :effect/set-mana-to-max {:widget :label
                                         :schema [:= true]
                                         :default-value true}
  (effect/usable? [_ {:keys [effect/source]}] source)
  (effect/text    [_ _effect-ctx]     (effect/text    [:effect/set-to-max :entity/mana]))
  (effect/useful? [_ effect-ctx _ctx] (effect/useful? [:effect/set-to-max :entity/mana] effect-ctx))
  (effect/txs     [_ effect-ctx]      (effect/txs     [:effect/set-to-max :entity/mana] effect-ctx)))

#_[:effect/set-to-max :entity/hp]
#_[:effect/set-to-max :entity/mana]

(defn- entity*->melee-damage [entity*]
  (let [strength (or (entity/stat entity* :stats/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect [{:keys [effect/source]}]
  [:effect/damage (entity*->melee-damage @source)])

(defcomponent :effect/melee-damage {}
  (effect/text [_ {:keys [effect/source] :as effect-ctx}]
    (str "Damage based on entity strength."
         (when source
           (str "\n" (effect/text (damage-effect effect-ctx)
                                  effect-ctx)))))

  (effect/usable? [_ effect-ctx]
    (effect/usable? (damage-effect effect-ctx) effect-ctx))

  (transact! [_ ctx]
    [(damage-effect ctx)]))

(defn- effective-armor-save [source* target*]
  (max (- (or (entity/stat target* :stats/armor-save) 0)
          (or (entity/stat source* :stats/armor-pierce) 0))
       0))

(comment
 ; broken
 (let [source* {:entity/stats {:stats/armor-pierce 0.4}}
       target* {:entity/stats {:stats/armor-save   0.5}}]
   (effective-armor-save source* target*))
 )

(defn- armor-saves? [source* target*]
  (< (rand) (effective-armor-save source* target*)))

(defn- apply-damage-modifiers [damage entity* stat]
  (update damage :damage/min-max ->effective-value stat (:entity/stats entity*)))

(comment
 (= (apply-damage-modifiers {:damage/min-max [5 10]}
                            {:entity/stats {:stats/modifiers {[:stats/damage-deal [:val :inc]] [1]}}}
                            :stats/damage-deal)
    #:damage{:min-max [6 10]})

 (= (apply-damage-modifiers {:damage/min-max [5 10]}
                            {:entity/stats {:stats/modifiers {[:stats/damage-deal [:max :mult]] [2]}}}
                            :stats/damage-deal)
    #:damage{:min-max [5 30]})

 (= (apply-damage-modifiers {:damage/min-max [5 10]}
                            {:entity/stats {:stats/modifiers nil}}
                            :stats/damage-receive)
    #:damage{:min-max [5 10]})
 )

(defn- effective-damage
  ([damage source*]
   (apply-damage-modifiers damage source* :stats/damage-deal))

  ([damage source* target*]
   (-> (effective-damage damage source*)
       (apply-damage-modifiers target* :stats/damage-receive))))

(comment
 ; broken
 (= (apply-damage-modifiers {:damage/min-max [3 10]}
                            {[:max :mult] 2
                             [:val :mult] 1.5
                             [:val :inc] 1
                             [:max :inc] 0})
    #:damage{:min-max [6 20]})

 (= (apply-damage-modifiers {:damage/min-max [6 20]}
                            {[:max :mult] 1
                             [:val :mult] 1
                             [:val :inc] -5
                             [:max :inc] 0})
    #:damage{:min-max [1 20]})

 (= (effective-damage {:damage/min-max [3 10]}
                      {:entity/stats {:stats/damage {:damage/deal {[:max :mult] [2]
                                                                   [:val :mult] [1.5]
                                                                   [:val :inc] [1]
                                                                   [:max :inc] [0]}}}}
                      {:entity/stats {:stats/damage {:damage/receive {[:max :mult] [1]
                                                                      [:val :mult] [1]
                                                                      [:val :inc] [-5]
                                                                      [:max :inc] [0]}}}})
    #:damage{:min-max [3 10]})
 )

(defn- damage->text [{[min-dmg max-dmg] :damage/min-max}]
  (str min-dmg "-" max-dmg " damage"))

(defcomponent :damage/min-max data/val-max-attr)

(defcomponent :effect/damage (data/map-attribute :damage/min-max)
  (effect/text [[_ damage] {:keys [effect/source]}]
    (if source
      (let [modified (effective-damage damage @source)]
        (if (= damage modified)
          (damage->text damage)
          (str (damage->text damage) "\nModified: " (damage->text modified))))
      (damage->text damage))) ; property menu no source,modifiers

  (effect/usable? [_ {:keys [effect/target]}]
    (and target
         ; TODO check for creature stats itself ? or just hp ?
         (entity/stat @target :stats/hp)))

  (transact! [[_ damage] {:keys [effect/source effect/target]}]
    (let [source* @source
          target* @target
          hp (entity/stat target* :stats/hp)]
      (cond
       (zero? (hp 0))
       []

       (armor-saves? source* target*)
       [[:tx/add-text-effect target "[WHITE]ARMOR"]] ; TODO !_!_!_!_!_!

       :else
       (let [{:keys [damage/min-max]} (effective-damage damage source* target*)
             dmg-amount (random/rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         [[:tx.entity/audiovisual (entity/position target*) :audiovisuals/damage]
          [:tx/add-text-effect target (str "[RED]" dmg-amount)]
          [:tx.entity/assoc-in target [:entity/stats :stats/hp 0] new-hp-val]
          [:tx/event target (if (zero? new-hp-val) :kill :alert)]])))))
