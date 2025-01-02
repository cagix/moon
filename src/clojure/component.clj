(ns clojure.component
  (:refer-clojure :exclude [apply])
  (:import (clojure.lang MultiFn)))

(defn- component-k->namespace [prefix k]
  (symbol (str prefix "." (namespace k) "." (name k))))

(defn- add-methods [system-vars ns-sym k & {:keys [optional?]}]
  (doseq [system-var system-vars
          :let [method-var (resolve (symbol (str ns-sym "/" (:name (meta system-var)))))]]
    (assert (or optional? method-var)
            (str "Cannot find required `" (:name (meta system-var)) "` function in " ns-sym))
    (when method-var
      (assert (keyword? k))
      (assert (var? method-var) (pr-str method-var))
      (alter-meta! method-var assoc :no-doc true)
      (let [system @system-var]
        (when (k (methods system))
          (println "WARNING: Overwriting method" (:name (meta method-var)) "on" k))
        (MultiFn/.addMethod system k method-var)))))

(defn install [component-systems ns-sym k]
  (require ns-sym)
  (add-methods (:required component-systems) ns-sym k)
  (add-methods (:optional component-systems) ns-sym k :optional? true)
  (let [ns (find-ns ns-sym)]
    (when (empty? (remove #(:no-doc (meta %)) (vals (ns-publics ns))))
      (alter-meta! ns assoc :no-doc true))))

#_(defn install [prefix systems components]
  (doseq [[k v] components]
    (install-component systems
                       (component-k->namespace prefix k)
                       k)))

(defmacro defsystem
  {:arglists '([name docstring?])}
  [name-sym & args]
  (let [docstring (if (string? (first args))
                    (first args))]
    `(defmulti ~name-sym
       ~(str "[[defsystem]]" (when docstring (str "\n\n" docstring)))
       (fn [[k#] & args#]
         k#))))

(def overwrite-warnings? false)

(defmacro defcomponent [k & sys-impls]
  `(do
    ~@(for [[sys & fn-body] sys-impls
            :let [sys-var (resolve sys)]]
        `(do
          (when (and overwrite-warnings?
                     (get (methods @~sys-var) ~k))
            (println "warning: overwriting defmethod" ~k "on" ~sys-var))
          (defmethod ~sys ~k ~(symbol (str (name (symbol sys-var)) "." (name k)))
            ~@fn-body)))
    ~k))

(defsystem create)
(defmethod create :default [[_ v] _context]
  v)

(defsystem create!)
(defmethod create! :default [_ eid c])

(defsystem destroy)
(defmethod destroy :default [_ eid c])

(defsystem tick)
(defmethod tick :default [_ eid c])

(defsystem render-below)
(defmethod render-below :default [_ entity c])

(defsystem render-default)
(defmethod render-default :default [_ entity c])

(defsystem render-above)
(defmethod render-above :default [_ entity c])

(defsystem render-info)
(defmethod render-info :default [_ entity c])

(defsystem enter)
(defmethod enter :default [_ c])

(defsystem exit)
(defmethod exit :default [_ c])

(defsystem cursor)
(defmethod cursor :default [_])

(defsystem clicked-inventory-cell)
(defmethod clicked-inventory-cell :default [_ cell c])

(defsystem clicked-skillmenu-skill)
(defmethod clicked-skillmenu-skill :default [_ skill c])

(defsystem draw-gui-view)
(defmethod draw-gui-view :default [_ c])

(defsystem manual-tick)
(defmethod manual-tick :default [_ c])

(defsystem applicable?)

(defsystem handle)

(defsystem useful?)
(defmethod useful? :default [_ _effect-ctx context] true)

(defsystem render-effect)
(defmethod render-effect :default [_ _effect-ctx context])

(defsystem apply)
(defsystem order)
(defsystem value-text)

(defsystem info)
(defmethod info :default [_ _context])
