(ns cdq.core
  (:require clojure.edn
            clojure.java.io
            [clojure.string :as str]
            clojure.walk)
  (:gen-class))

(defn execute! [[f params]]
  (f params))

(defn- java-class-symbol? [s]
  (let [last-segment (last (str/split s #"\."))]
    (Character/isUpperCase ^Character (first last-segment))))

(defn- var-symbol? [sym]
  (namespace sym))

(defn req [form]
  (if (symbol? form)
    (if (var-symbol? form)
      (requiring-resolve form)
      (if (java-class-symbol? (str form))
        (do
         (try
          (eval form)
          (catch clojure.lang.Compiler$CompilerException e
            ; clojure generated types (e.g. records), need to require the namespace first
            (require (symbol (str/join "." (drop-last (str/split (str form) #"\.")))))
            (eval form))))
        (do
         (require form)
         form))) ; otherwise clojure namespace
    form))

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

(defn load! [path]
  (->> path
       clojure.java.io/resource
       slurp
       clojure.edn/read-string
       (clojure.walk/postwalk req)
       (run! execute!)))

(defn -main [path]
  (load! path))
