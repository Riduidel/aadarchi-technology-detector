/**
 * @author Sylvain Gougouzian
 * @version 1.0
 * @description Analyse a github project
 */

import { top1000 as topPHPPackages } from "./packagistorg/top1000";
import { top1000 as topJSPackages } from "./npmjscom/top1000";
import { saveJson } from "./utils";

//const topPHP = await topPHPPackages();
const topJS = await topJSPackages();

saveJson("outputs/artifact.json", { ...topJS });
