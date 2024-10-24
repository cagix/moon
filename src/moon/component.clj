(ns moon.component
  (:refer-clojure :exclude [meta])
  (:require [clojure.string :as str]
            [gdl.utils :refer [index-of]]))

(def systems {})

(defmacro defsystem
  ([sys-name]
   `(defsystem ~sys-name nil ['_]))

  ([sys-name params-or-doc]
   (let [[doc params] (if (string? params-or-doc)
                        [params-or-doc ['_]]
                        [nil params-or-doc])]
     `(defsystem ~sys-name ~doc ~params)))

  ([sys-name docstring params]
   (when (zero? (count params))
     (throw (IllegalArgumentException. "First argument needs to be component.")))
   (when-let [avar (resolve sys-name)]
     (println "WARNING: Overwriting defsystem:" avar))
   `(do
     (defmulti ~(vary-meta sys-name assoc :params (list 'quote params))
       ~(str "[[defsystem]] `" params "`" (when docstring (str "\n\n" docstring)))
       (fn [[k#] & _args#] k#))
     (alter-var-root #'systems assoc ~(str (ns-name *ns*) "/" sys-name) (var ~sys-name))
     (var ~sys-name))))

(def meta {})

(defn defc*
  [k attr-map]
  (when (meta k)
    (println "WARNING: Overwriting defc" k "attr-map"))
  (alter-var-root #'meta assoc k attr-map))

(defmacro defc [k & sys-impls]
  (let [attr-map? (not (list? (first sys-impls)))
        attr-map  (if attr-map? (first sys-impls) {})
        sys-impls (if attr-map? (rest sys-impls) sys-impls)
        let-bindings (:let attr-map)
        attr-map (dissoc attr-map :let)]
    `(do
      (when ~attr-map?
        (defc* ~k ~attr-map))
      #_(alter-meta! *ns* #(update % :doc str "\n* defc `" ~k "`"))
      ~@(for [[sys & fn-body] sys-impls
              :let [sys-var (resolve sys)
                    sys-params (:params (clojure.core/meta sys-var))
                    fn-params (first fn-body)
                    fn-exprs (rest fn-body)]]
          (do
           (when-not sys-var
             (throw (IllegalArgumentException. (str sys " does not exist."))))
           (when-not (= (count sys-params) (count fn-params)) ; defmethods do not check this, that's why we check it here.
             (throw (IllegalArgumentException.
                     (str sys-var " requires " (count sys-params) " args: " sys-params "."
                          " Given " (count fn-params)  " args: " fn-params))))
           `(do
             (assert (keyword? ~k) (pr-str ~k))
             (alter-var-root #'meta assoc-in [~k :params ~(name (symbol sys-var))] (quote ~fn-params))
             (when (get (methods @~sys-var) ~k)
               (println "WARNING: Overwriting defc" ~k "on" ~sys-var))
             (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
               [& params#]
               (let [~(if let-bindings let-bindings '_) (get (first params#) 1) ; get because maybe component is just [:foo] without v.
                     ~fn-params params#]
                 ~@fn-exprs)))))
      ~k)))

(defsystem create)

(defsystem handle)

(defn ->handle [txs]
  (doseq [tx txs]
    (when-let [result (try (cond (not tx) nil
                                 (fn? tx) (tx)
                                 :else (handle tx))
                           (catch Throwable t
                             (throw (ex-info "Error with transactions" {:tx tx} t))))]
      (->handle result))))

(defsystem info)
(defmethod info :default [_])

(def ^:private info-text-k-order [:property/pretty-name
                                  :skill/action-time-modifier-key
                                  :skill/action-time
                                  :skill/cooldown
                                  :skill/cost
                                  :skill/effects
                                  :creature/species
                                  :creature/level
                                  :stats/hp
                                  :stats/mana
                                  :stats/strength
                                  :stats/cast-speed
                                  :stats/attack-speed
                                  :stats/armor-save
                                  :entity/delete-after-duration
                                  :projectile/piercing?
                                  :entity/projectile-collision
                                  :maxrange
                                  :entity-effects])

(defn- sort-k-order [components]
  (sort-by (fn [[k _]] (or (index-of k info-text-k-order) 99))
           components))

(defn- remove-newlines [s]
  (let [new-s (-> s
                  (str/replace "\n\n" "\n")
                  (str/replace #"^\n" "")
                  str/trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

(declare ^:dynamic *info-text-entity*)

(defn ->info
  "Recursively generates info-text via [[info]]."
  [components]
  (->> components
       sort-k-order
       (keep (fn [{v 1 :as component}]
               (str (try (binding [*info-text-entity* components]
                           (info component))
                         (catch Throwable t
                           ; calling from property-editor where entity components
                           ; have a different data schema than after component/create
                           ; and info-text might break
                           (pr-str component)))
                    (when (map? v)
                      (str "\n" (->info v))))))
       (str/join "\n")
       remove-newlines))
