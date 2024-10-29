; 1.  src/moon/modifiers.clj:
; * comments there fix/cleanup/text.


; 2. moon.entity.modifiers-test:
; I am testing the wrong thing here !
; too many imports !
; the transactuin should just return an :e/update form !

;# add tests for val-max ! damage !
;(moon.operation.val-max)
;see comments with =

; Also these comments removed:

(defn- modified-value [{:keys [entity/modifiers]} modifier-k base-value]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (->> modifiers
       modifier-k
       ; here just call sum first
       ; then no need apply +
       ; and can call a ops/apply fn which does the sort
       ; so a mods/apply here ? no wait
       (sort-by op/order)
       (reduce (fn [base-value [operation-k values]]
                 (op/apply [operation-k (apply + values)] base-value))
               base-value)))

; not possible to test that anonymous fn ?
; pass f to :e/update ?
; its just e/assoc ?!
; => I need a tx' helper or ?
(defn update-mods [[_ eid mods] f]
  [[:e/update eid :entity/modifiers #(f % mods)]])

; just add-remove !
; [:entity/modifiers eid :add    modifiers]
; [:entity/modifiers eid :remove modifiers]
(defc :tx/apply-modifiers   (component/handle [this] (update-mods this mods/add)))
(defc :tx/reverse-modifiers (component/handle [this] (update-mods this mods/remove)))


; unused:

; For now no green/red color for positive/negative numbers
; as :stats/damage-receive negative value would be red but actually a useful buff
; -> could give damage reduce 10% like in diablo 2
; and then make it negative .... @ applicator
(def ^:private positive-modifier-color "[MODIFIER_BLUE]" #_"[LIME]")
(def ^:private negative-modifier-color "[MODIFIER_BLUE]" #_"[SCARLET]")


; so here we just do i tlike modifiers
; multiple stat we can affect
; with just multiple :effect-k ...
(defc :base/stat-effect
  (component/info [[k operations]]
    (str/join "\n"
              (for [operation operations]
                (str (mods/op-info-text operation) " " (k->pretty-name k)))))

  (component/applicable? [[k _]]
    (and effect/target
         (entity/stat @effect/target (stat-k k))))

  (component/useful? [_]
    true)

  (component/handle [[effect-k operations]]
    (let [stat-k (stat-k effect-k)]
      ; I don't understand how we can apply operations on effective-value
      ; won't the modifiers get changed then
      ; shouldnt we work on  base value ?
      (when-let [effective-value (entity/stat @effect/target stat-k)]
        [[:e/assoc effect/target stat-k (reduce (fn [value operation]
                                                  (op/apply operation value))
                                                effective-value
                                                operations)]]))))

; for example + 100% HP
; so you have 10 and then makes it 20 EFFECTIVE_VALUE
; now I give you 5 damage and youre at 15
; so I change your BASE_VALUE to 15
; then with the +100% HP still there you suddenly have 30 HP?
; how does this even work ?

; => thats why I need automatede tests also for val-max ops !!!!!

; => because we don't have modifiers for the VAL part of HP /mana...

; we cant run effects on modified stuffs ?
; that would mean just adding one more modifier permanently...

; but those totally permanent modifiers go together with a status component
; forr example for spiderweb ?

