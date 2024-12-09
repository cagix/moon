(ns anvil.info
  (:require [clojure.component :refer [defsystem]]
            [clojure.string :as str]
            [clojure.utils :refer [index-of]]))

(declare k->colors
         k-order)

(defn- apply-color [k info-text]
  (if-let [color (k->colors k)]
    (str "[" color "]" info-text "[]")
    info-text))

(defn- sort-k-order [components]
  (sort-by (fn [[k _]] (or (index-of k k-order) 99))
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

(defsystem segment)
(defmethod segment :default [_])

(defn text [components]
  (->> components
       sort-k-order
       (keep (fn [{k 0 v 1 :as component}]
               (str (try (binding [*info-text-entity* components]
                           (apply-color k (segment component)))
                         (catch Throwable t
                           ; calling from property-editor where entity components
                           ; have a different data schema than after component/create
                           ; and info-text might break
                           (pr-str component)))
                    (when (map? v)
                      (str "\n" (text v))))))
       (str/join "\n")
       remove-newlines))
