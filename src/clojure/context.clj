(ns clojure.context
  (:require cdq.graphics
            [clojure.scene2d.stage :as stage]
            [clojure.utils :refer [with-err-str]]
            [clojure.db :as db]
            [clojure.error :refer [pretty-pst]]
            [clojure.ui :as ui]))

(def stage :clojure.context/stage)

(defn build [{:keys [clojure/db] :as c} id]
  (db/build db id c))

(defn build-all [{:keys [clojure/db] :as c} property-type]
  (db/build-all db property-type c))

(defn add-actor [{:keys [clojure.context/stage]} actor]
  (stage/add-actor stage actor))

(defn reset-stage [{:keys [clojure.context/stage]} new-actors]
  (stage/clear stage)
  (run! #(stage/add-actor stage %) new-actors))

(defn mouse-on-actor? [{:keys [clojure.context/stage] :as c}]
  (let [[x y] (cdq.graphics/mouse-position c)]
    (stage/hit stage x y true)))

(defn error-window [c throwable]
  (pretty-pst throwable)
  (add-actor c
   (ui/window {:title "Error"
               :rows [[(ui/label (binding [*print-level* 3]
                                   (with-err-str
                                     (clojure.repl/pst throwable))))]]
               :modal? true
               :close-button? true
               :close-on-escape? true
               :center? true
               :pack? true})))
