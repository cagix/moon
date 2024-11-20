(ns moon.system
  (:require [clojure.string :as str]))

(defmacro defsystem
  {:arglists '([name docstring? params?])}
  [name-sym & args]
  (let [docstring (if (string? (first args))
                    (first args))
        params (if (string? (first args))
                 (second args)
                 (first args))
        params (if (nil? params)
                 '[_]
                 params)]
    (when (zero? (count params))
      (throw (IllegalArgumentException. "First argument needs to be component.")))
    (when-let [avar (resolve name-sym)]
      (println "WARNING: Overwriting defsystem:" avar))
    `(defmulti ~(vary-meta name-sym assoc :params (list 'quote params))
       ~(str "[[defsystem]] `" (str params) "`"
             (when docstring (str "\n\n" docstring)))
       (fn [[k#] & _args#]
         k#))))

(defmacro defmethods [k & method-impls]
  `(do
    ~@(for [[sys & fn-body] method-impls
            :let [sys-var (resolve sys)]]
        `(do
          (when (get (methods @~sys-var) ~k)
            (println "WARNING: Overwriting defmethod" ~k "on" ~sys-var))
          (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
            ~@fn-body)))
    ~k))

(declare ^:dynamic *k*)

(def ^:private no-doc? true)

; check fn-params ... ? compare with sys-params ?
; #_(first (:arglists (meta #'render)))
(defn- add-method [system-var k avar]
  (assert (keyword? k))
  (assert (var? avar) (pr-str avar))
  (if no-doc?
    (alter-meta! avar assoc :no-doc true)
    (alter-meta! avar update :doc str "\n installed as defmethod for " system-var))
  (let [system @system-var]
    (when (k (methods system))
      (println "WARNING: Overwriting method" (:name (meta avar)) "on" k))
    (clojure.lang.MultiFn/.addMethod system k (fn call-method [[k & vs] & args]
                                                (binding [*k* k]
                                                  (apply avar (into (vec vs) args)))))))

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var system-vars
          :let [method-var (ns-resolve ns-sym (:name (meta system-var)))]]
    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym))
    (when method-var
      (add-method system-var k method-var))))

(defn- ns-publics-without-no-doc? [ns]
  (some #(not (:no-doc (meta %))) (vals (ns-publics ns))))

(defn- install* [component-systems ns-sym k]
  (require ns-sym)
  (add-methods (:required component-systems) ns-sym k)
  (add-methods (:optional component-systems) ns-sym k :optional? true)
  (let [ns (find-ns ns-sym)]
    (if (and no-doc? (not (ns-publics-without-no-doc? ns)))
      (alter-meta! ns assoc :no-doc true)
      (alter-meta! ns update :doc str "\n component: `" k "`"))))

(defn namespace->component-key [prefix ns-str]
   (let [ns-parts (-> ns-str
                      (str/replace prefix "")
                      (str/split #"\."))]
     (keyword (str/join "." (drop-last ns-parts))
              (last ns-parts))))

(comment
 (and (= (namespace->component-key "moon.effects.projectile")
         :effects/projectile)
      (= (namespace->component-key "moon.effects.target.convert")
         :effects.target/convert)))

(defn install
  ([component-systems ns-sym]
   (install* component-systems
             ns-sym
             (namespace->component-key #"^moon." (str ns-sym))))
  ([component-systems ns-sym k]
   (install* component-systems ns-sym k)))

(defn install-all [component-systems ns-syms]
  (doseq [ns-sym ns-syms]
    (install component-systems ns-sym)))
