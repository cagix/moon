(ns gdl.graphics.shape-drawer
  (:require [space.earlygrey.shape-drawer :as sd])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defprotocol PShapeDrawer
  (set-color! [_ color])
  (with-line-width [_ width draw-fn])
  (arc! [_ center-x center-y radius start-radians radians])
  (circle! [_ x y radius])
  (ellipse! [_ x y radius-x radius-y])
  (filled-circle! [_ x y radius])
  (filled-ellipse! [_ x y radius-x radius-y])
  (filled-rectangle! [_ x y w h])
  (line! [_ sx sy ex ey])
  (rectangle! [_ x y w h])
  (sector! [_ center-x center-y radius start-radians radians]))

(def create sd/create)

(extend ShapeDrawer
  PShapeDrawer
  {:set-color! sd/set-color!
   :with-line-width sd/with-line-width
   :arc! sd/arc!
   :circle! sd/circle!
   :ellipse! sd/ellipse!
   :filled-circle! sd/filled-circle!
   :filled-ellipse! sd/filled-ellipse!
   :filled-rectangle! sd/filled-rectangle!
   :line! sd/line!
   :rectangle! sd/rectangle!
   :sector! sd/sector!})
