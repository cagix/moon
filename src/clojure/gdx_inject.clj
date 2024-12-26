(in-ns 'clojure.core)

(require '[clojure.gdx.graphics.color :as color]
         '[clojure.gdx.utils.screen-utils :as screen-utils]
         '[gdl.ui.group :as group]
         '[gdl.ui.actor :as actor])

(defn clear-screen [] ; I am sure also needs context ... pass gl directly ! check implementation !
  (screen-utils/clear color/black))

(def children group/children)

(def toggle-visible! actor/toggle-visible!)
(def visible?        actor/visible?)
(def set-visible     actor/set-visible)
