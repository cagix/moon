(ns master.yoda)

(defn execute! [[f params]]
  (f params))

(defn dispatch [[to-eval mapping]]
  (->> (to-eval)
       (get mapping)
       (run! execute!)))

(defn req [form]
  (if (symbol? form)
    (if (namespace form)
      (requiring-resolve form)
      (try (require form)
           form
           (catch Exception e ; Java classes
             form)))
    form))

(defn provide [impls]
  (doseq [[atype implementation-ns protocol] impls]
    (let [atype (eval atype)
          protocol @protocol
          method-map (update-vals (:sigs protocol)
                                  (fn [{:keys [name]}]
                                    (requiring-resolve (symbol (str implementation-ns "/" name)))))]
      (extend atype protocol method-map))))

(defn render* [ctx render-element]
  (if (vector? render-element)
    (let [[f params] render-element]
      (f ctx params))
    (let [f render-element]
      (f ctx))))

(defn create!-reset! [{:keys [state-atom initial-context create-fns]}]
  (reset! @state-atom (reduce render*
                              (execute! initial-context)
                              create-fns)))

(defn const* [_ctx params]
  params)

(defn assoc* [ctx [k [f params]]]
  (assoc ctx k (f ctx params)))

(defn render-when-not [ctx [ks render-fns]]
  (if (get-in ctx ks)
    ctx
    (reduce render* ctx render-fns)))

(comment
 (= (let [->graphics (fn [ctx params] (str :GRAPHICS "-" params))
          ->audio    (fn [ctx params] (str :AUDIO "-" params))]
      (reduce render*
              {:initial-context :foobar}
              [[assoc* [:ctx/graphics [->graphics :GDX]]]
               [assoc* [:ctx/audio    [->audio "OpenAL"]]]]))
    {:initial-context :foobar, :ctx/graphics ":GRAPHICS-:GDX", :ctx/audio ":AUDIO-OpenAL"})
 )

(defn render-swap! [{:keys [state-atom render-fns]}]
  (swap! @state-atom (fn [ctx]
                       (reduce render* ctx render-fns))))

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var system-vars
          :let [method-var (resolve (symbol (str ns-sym "/" (:name (meta system-var)))))]]
    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym))
    (when method-var
      (assert (keyword? k))
      (assert (var? method-var) (pr-str method-var))
      (let [system @system-var]
        (when (k (methods system))
          (println "WARNING: Overwriting method" (:name (meta method-var)) "on" k))
        (println "addMethod system " system-var " k " k " method-var " method-var)
        (clojure.lang.MultiFn/.addMethod system k method-var)))))

(defn install-methods [{:keys [required optional]} ns-sym k]
  (require ns-sym)
  (add-methods required ns-sym k)
  (add-methods optional ns-sym k :optional? true))

; TODO  install also editor widgets
; also defmethod tx/do! can separate ....
; or draws
; grep 'defmethod'

(require 'cdq.effect)

(let [effect-multis {:required [#'cdq.effect/applicable?
                                #'cdq.effect/handle]
                     :optional [#'cdq.effect/useful?
                                #'cdq.effect/render]}]
  (doseq [[ns-sym k] [
                      ['cdq.effects.audiovisual
                       :effects/audiovisual]
                      ['cdq.effects.projectile
                       :effects/projectile]
                      ['cdq.effects.sound
                       :effects/sound]
                      ['cdq.effects.spawn
                       :effects/spawn]
                      ['cdq.effects.target-all
                       :effects/target-all]
                      ['cdq.effects.target-entity
                       :effects/target-entity]
                      ['cdq.effects.target.audiovisual
                       :effects.target/audiovisual]
                      ['cdq.effects.target.convert
                       :effects.target/convert]
                      ['cdq.effects.target.damage
                       :effects.target/damage]
                      ['cdq.effects.target.kill
                       :effects.target/kill]
                      ['cdq.effects.target.melee-damage
                       :effects.target/melee-damage]
                      ['cdq.effects.target.spiderweb
                       :effects.target/spiderweb]
                      ['cdq.effects.target.stun
                       :effects.target/stun]
                      ]]
    (install-methods effect-multis ns-sym k)))
