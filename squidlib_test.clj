[com.squidpony/squidlib-util "3.0.6"]

(comment

 (import 'squidpony.squidgrid.mapping.ConnectingMapGenerator)
 (doseq [row (.generate (ConnectingMapGenerator.))]
   (println)
   (doseq [col (seq row)]
     (print col)))

 )
