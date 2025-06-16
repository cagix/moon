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
