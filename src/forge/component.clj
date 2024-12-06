(ns forge.component
  (:require [clojure.string :as str]
            [forge.system :refer [defsystem]]
            [forge.utils :refer [index-of]]))

(defsystem info)
(defmethod info :default [_])

(declare info-color
         info-text-k-order)

(defn- apply-color [k info-text]
  (if-let [color (info-color k)]
    (str "[" color "]" info-text "[]")
    info-text))

(defn- sort-k-order [components]
  (sort-by (fn [[k _]] (or (index-of k info-text-k-order) 99))
           components))

(declare ^:dynamic *info-text-entity*)

(defn- remove-newlines [s]
  (let [new-s (-> s
                  (str/replace "\n\n" "\n")
                  (str/replace #"^\n" "")
                  str/trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

(defn info-text [components]
  (->> components
       sort-k-order
       (keep (fn [{k 0 v 1 :as component}]
               (str (try (binding [*info-text-entity* components]
                           (apply-color k (info component)))
                         (catch Throwable t
                           ; calling from property-editor where entity components
                           ; have a different data schema than after component/create
                           ; and info-text might break
                           (pr-str component)))
                    (when (map? v)
                      (str "\n" (info-text v))))))
       (str/join "\n")
       remove-newlines))
