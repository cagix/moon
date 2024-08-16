(ns entity.stats
  (:require [clojure.string :as str]
            [clojure.math :as math]
            [malli.core :as m]
            [gdx.graphics.color :as color]
            [utils.random :as random]
            [data.val-max :refer [val-max-schema val-max-ratio lower-than-max? set-to-max]]
            [core.component :refer [defcomponent]]
            [core.data :as data]
            [api.effect :as effect]
            [api.entity :as entity]
            [api.graphics :as g]
            [api.tx :refer [transact!]]
            [context.ui.config :refer (hpbar-height-px)]))

; TODO default-values dont make sense
; e.g. in magic creatures have explicitly 0 power
; here they have 0 power but implicitly
; also the code doesnt want to know e.g. armor calculations
; => make it explicit ...
; only attack/cast-speed is quite uninteresting ? could hide ?

(defn- conj-value [value]
  (fn [values]
    (conj values value)))

; TODO if no values remaining -> remove the modifier itself
; or ignore it if values total is 0 at infotext.
(defn- remove-value [value]
  (fn [values]
    {:post [(= (count %) (dec (count values)))]}
    (remove #{value} values)))

(defn- txs-update-modifiers [entity modifiers f]
  (for [[modifier-k operations-m] modifiers
        [operation-k value] operations-m]
    [:tx.entity/update-in entity [:entity/stats :stats/modifiers modifier-k operation-k] (f value)]))

(comment
 (= (txs-update-modifiers :entity
                         {:modifier/hp {:op/max-inc 5
                                        :op/max-mult 0.3}
                          :modifier/movement-speed {:op/mult 0.1}}
                         :fn/add-value)
    [[:tx.entity/update-in :entity [:entity/stats :stats/modifiers :modifier/hp :op/max-inc] :fn/add-value]
     [:tx.entity/update-in :entity [:entity/stats :stats/modifiers :modifier/hp :op/max-mult] :fn/add-value]
     [:tx.entity/update-in :entity [:entity/stats :stats/modifiers :modifier/movement-speed :op/mult] :fn/add-value]])
 )

; TODO plural
(defmethod transact! :tx/apply-modifier [[_ entity modifiers] ctx]
  (txs-update-modifiers entity modifiers conj-value))

; TODO plural
(defmethod transact! :tx/reverse-modifier [[_ entity modifiers] ctx]
  (txs-update-modifiers entity modifiers remove-value))

(defcomponent :op/inc {:widget :text-field
                       :schema number?}) ; TODO for strength its only int increase, for movement-speed different .... ??? how to manage this ?

(defcomponent :op/mult {:widget :text-field
                        :schema number?})

(defcomponent :op/max-inc {:widget :text-field
                           :schema int?})

(defcomponent :op/max-mult {:widget :text-field
                            :schema number?})

(defcomponent :op/val-inc {:widget :text-field
                           :schema int?})

(defcomponent :op/val-mult {:widget :text-field
                            :schema number?})

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

(defn- k->pretty-name [k]
  (str/capitalize (name k)))

(defn info-text [modifier-k operation-k value]
  (str (+? value)
       (case operation-k
         :op/inc (str value " ")
         :op/mult (str (->percent value) " ")
         :op/val-inc (str value " minimum ")
         :op/max-inc (str value " maximum ")
         :op/val-mult (str (->percent value) " minimum ")
         :op/max-mult (str (->percent value) " maximum "))
       (k->pretty-name modifier-k)
       "[]"))

(defmulti apply-operation (fn [operation-k _base-value _value]
                            operation-k))

(defmethod apply-operation :op/inc [_ base-value value]
  (+ base-value value))

(defmethod apply-operation :op/mult [_ base-value value]
  (* base-value (inc value)))

(defn- ->pos-int [v]
  (-> v int (max 0)))

(defn- val-max-op-k->parts [op-k]
  (mapv keyword (str/split (name op-k) #"-")))

(comment
 (= (val-max-op-k->parts :op/val-inc) [:val :inc])
 )

(defmethod apply-operation :op/val-max [operation-k val-max value]
  {:post [(m/validate val-max-schema %)]}
  (assert (m/validate val-max-schema val-max)
          (str "Invalid val-max-schema: " (pr-str val-max)))
  (let [[val-or-max inc-or-mult] (val-max-op-k->parts operation-k)
        f #(apply-operation (keyword "op" (name inc-or-mult)) % value)
        [v mx] (update val-max (case val-or-max :val 0 :max 1) f)
        v  (->pos-int v)
        mx (->pos-int mx)]
    (case val-or-max
      :val [v (max v mx)]
      :max [(min v mx) mx])))

(derive :op/max-inc :op/val-max)
(derive :op/max-mult :op/val-max)
(derive :op/val-inc :op/val-max)
(derive :op/val-mult :op/val-max)

(comment
 (and
  (= (apply-operation :op/val-inc [5 10] 30) [35 35])
  (= (apply-operation :op/max-mult [5 10] -0.5) [5 5])
  (= (apply-operation :op/val-mult [5 10] 2) [15 15])
  (= (apply-operation :op/val-mult [5 10] 1.3) [11 11])
  (= (apply-operation :op/max-mult [5 10] -0.8) [1 1])
  (= (apply-operation :op/max-mult [5 10] -0.9) [0 0]))
 )

(defn- op-order [[operation-k _values]]
  (case operation-k
    :op/inc 0
    :op/mult 1
    :op/val-inc 0
    :op/val-mult 1
    :op/max-inc 0
    :op/max-mult 1))

; TODO
; * bounds (action-speed not <=0 , not value '-1' e.g.)/schema/values/allowed operations
; * take max-movement-speed into account @ :stats/movement-speed
(defn- ->effective-value [base-value modifier-k stats]
  (->> stats
       :stats/modifiers
       modifier-k
       (sort-by op-order)
       (reduce (fn [base-value [operation-k values]]
                 (apply-operation operation-k base-value (apply + values)))
               base-value)))

(comment
 (and
  (= (->effective-value [5 10]
                        :modifier/damage-deal
                        {:stats/modifiers {:modifier/damage-deal {:op/val-inc [30]}
                                           :stats/fooz-barz {:op/babu [1 2 3]}}})
     [35 35])
  (= (->effective-value [5 10]
                        :modifier/damage-deal
                        {:stats/modifiers {}})
     [5 10])
  (= (->effective-value [100 100]
                        :stats/hp
                        {:stats/modifiers {:stats/hp {:op/max-inc [10 1]
                                                      :op/max-mult [0.5]}}})
     [100 166])
  (= (->effective-value 3
                        :stats/movement-speed
                        {:stats/modifiers {:stats/movement-speed {:op/inc [2]
                                                                  :op/mult [0.1 0.2]}}})
     6.5))
 )

(defn defmodifier [modifier-k operations]
  (defcomponent modifier-k (data/components operations)))

(defn defstat [stat-k metam & {:keys [operations]}]
  (defcomponent stat-k metam)
  (defmodifier (keyword "modifier" (name stat-k)) operations))

(defstat :stats/hp data/pos-int-attr
  :operations [:op/max-inc :op/max-mult])

(defstat :stats/mana data/nat-int-attr
  :operations [:op/max-inc :op/max-mult])

; TODO only mult required  ( ? )
(defstat :stats/movement-speed data/pos-attr
  :operations [:op/inc :op/mult])

(defstat :stats/strength data/nat-int-attr
  :operations [:op/inc])

(let [doc "action-time divided by this stat when a skill is being used.
          Default value 1.

          For example:
          attack/cast-speed 1.5 => (/ action-time 1.5) => 150% attackspeed."
      skill-speed-stat (assoc data/pos-attr :doc doc)
      operations [:op/inc]]
  (defstat :stats/cast-speed   skill-speed-stat :operations operations)
  (defstat :stats/attack-speed skill-speed-stat :operations operations))

(defstat :stats/armor-save {:widget :text-field :schema number?}
  :operations [:op/inc])

(defstat :stats/armor-pierce {:widget :text-field :schema number?}
  :operations [:op/inc])

; TODO kommt aufs gleiche raus if we have +1 min damage or +1 max damage?
; just inc/mult ?
; or even mana/hp does it make a difference ?
(defmodifier :modifier/damage-deal [:op/val-inc :op/val-mult :op/max-inc :op/max-mult])

(defmodifier :modifier/damage-receive [:op/inc :op/mult])

(defcomponent :stats/modifiers (data/components [:modifier/damage-deal
                                                 :modifier/damage-receive]))

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


; TODO make it all required like in MTG
; then can make effects, e.g. destroy target creature with strength < 3
; mana needed ? idk
; cast-speed/attack-speed also idk (maybe dexterity, intelligence)
; armor-pierce into an effect.... like in wh40k.


(def ^:private stats-keywords
  ; hp/mana/movement-speed given for all entities ?
  ; but maybe would be good to have 'required' key ....
  ; also default values defined with the stat itself ?
  ; armor-pierce move out of here ...
  [:stats/hp ; made optional but bug w. projectile transact damage anyway not checking usable?
   :stats/mana ; made optional @ active-skill
   :stats/movement-speed
   :stats/strength  ; default value 0
   :stats/cast-speed ; has default value 1 @ entity.stateactive-skill
   :stats/attack-speed ; has default value 1 @ entity.stateactive-skill
   :stats/armor-save ; default-value 0
   :stats/armor-pierce ; default-value 0
   ])

; TODO use data/components
; and pass :optional or not ....
; see where else we use data/components ....
; or make it into the component itself if optional or not
; but depends on usage ?
; possible different usage component at different places?
; e.g. audiovisual ?

; or make even hp or movement-speed optional
; for example then cannot attack the princess ... ?

; then the create component here have to move into the component itself.
(defn- build-modifiers [modifiers]
  (into {} (for [[modifier-k operations] modifiers]
             [modifier-k (into {} (for [[operation-k value] operations]
                                    [operation-k [value]]))])))

(comment
 (=
  {:modifier/damage-receive {:op/mult [-0.9]}}
  (build-modifiers {:modifier/damage-receive {:op/mult -0.9}}))
 )

(defcomponent :entity/stats (data/components-attribute :stats)
  (entity/create-component [[_ stats] _components _ctx]
    (-> stats
        (update :stats/hp (fn [hp] (when hp [hp hp])))
        (update :stats/mana (fn [mana] (when mana [mana mana])))
        (update :stats/modifiers build-modifiers)))

  ; TODO proper texts...
  ; HP color based on ratio like hp bar samey (take same color definitions etc.)
  ; mana color same in the whole app
  ; red positive/green negative
  (entity/info-text [[_ {:keys [stats/modifiers] :as stats}] _ctx]
    ; TODO use component/info-text
    ; each stat derives from :component/stat
    ; and stat/modifiers has its own info-text method
    ; => only those which are there ....
    ; which should be there or not ????
    (str (str/join "\n"
                   (for [stat-k stats-keywords
                         :let [base-value (stat-k stats)]
                         :when base-value]
                     (str (k->pretty-name stat-k) ": " (->effective-value base-value stat-k stats))))
         (when (seq modifiers)
           (str "\n"
                (str/join "\n"
                          (for [[modifier-k operations] modifiers
                                [operation-k values] operations]
                            (info-text modifier-k operation-k (reduce + values))))))))

  #_(comment
    (let [ctx @app.state/current-context
          entity (api.context/get-entity ctx 55)]
      (-> @entity
          :entity/stats
          :stats/modifiers
          str)))

  (entity/render-info [_
                       {{:keys [width half-width half-height]} :entity/body
                        :keys [entity/mouseover?] :as entity*}
                       g
                       _ctx]
    (when-let [hp (entity/stat entity* :stats/hp)]
      (let [ratio (val-max-ratio hp)
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
                                     (hpbar-color ratio))))))))

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

(defn- ->effective-damage [damage source*]
  (update damage :damage/min-max ->effective-value :modifier/damage-deal (:entity/stats source*)))

(comment
 (let [->stats (fn [mods] {:entity/stats {:stats/modifiers mods}})]
   (and
    (= (->effective-damage {:damage/min-max [5 10]}
                           (->stats {:modifier/damage-deal {:op/val-inc [1 5 10]
                                                            :op/val-mult [0.2 0.3]
                                                            :op/max-mult [1]}}))
       #:damage{:min-max [31 62]})

    (= (->effective-damage {:damage/min-max [5 10]}
                           (->stats {:modifier/damage-deal {:op/val-inc [1]}}))
       #:damage{:min-max [6 10]})

    (= (->effective-damage {:damage/min-max [5 10]}
                           (->stats {:modifier/damage-deal {:op/max-mult [2]}}))
       #:damage{:min-max [5 30]})

    (= (->effective-damage {:damage/min-max [5 10]}
                           (->stats nil))
       #:damage{:min-max [5 10]}))))

(defn- damage->text [{[min-dmg max-dmg] :damage/min-max}]
  (str min-dmg "-" max-dmg " damage"))

(defcomponent :damage/min-max data/val-max-attr)

(defcomponent :effect/damage (data/map-attribute :damage/min-max)
  (effect/text [[_ damage] {:keys [effect/source]}]
    (if source
      (let [modified (->effective-damage damage @source)]
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
       (let [{:keys [damage/min-max]} (->effective-damage damage source*)
             ;_ (println "\nmin-max:" min-max)
             dmg-amount (random/rand-int-between min-max)
             ;_ (println "dmg-amount: " dmg-amount)
             dmg-amount (->pos-int (->effective-value dmg-amount :modifier/damage-receive (:entity/stats target*)))
             ;_ (println "effective dmg-amount: " dmg-amount)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         [[:tx.entity/audiovisual (entity/position target*) :audiovisuals/damage]
          [:tx/add-text-effect target (str "[RED]" dmg-amount)]
          [:tx.entity/assoc-in target [:entity/stats :stats/hp 0] new-hp-val]
          [:tx/event target (if (zero? new-hp-val) :kill :alert)]])))))
