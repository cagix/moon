(ns cdq.core)

(defn execute! [[f params]]
  (f params))

(defn render* [ctx render-element]
  (if (vector? render-element)
    (let [[f params] render-element]
      (f ctx params))
    (let [f render-element]
      (f ctx))))

(defn const* [_ctx params]
  params)

(defn assoc* [ctx [k [f params]]]
  (assoc ctx k (f ctx params)))

(comment
 (= (let [->graphics (fn [ctx params] (str :GRAPHICS "-" params))
          ->audio    (fn [ctx params] (str :AUDIO "-" params))]
      (reduce render*
              {:initial-context :foobar}
              [[assoc* [:ctx/graphics [->graphics :GDX]]]
               [assoc* [:ctx/audio    [->audio "OpenAL"]]]]))
    {:initial-context :foobar, :ctx/graphics ":GRAPHICS-:GDX", :ctx/audio ":AUDIO-OpenAL"})
 )
