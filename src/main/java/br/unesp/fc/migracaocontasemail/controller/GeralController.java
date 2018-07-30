/*
 * MIT License
 *
 * Copyright (c) 2018 Demitrius Belai
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package br.unesp.fc.migracaocontasemail.controller;

import br.unesp.fc.migracaocontasemail.authentication.Usuario;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GeralController {

    private static final Logger log = LoggerFactory.getLogger(GeralController.class);

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model) {
        model.addAttribute("check", "");
        return "index";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public String iniciar(Model model,
            HttpServletRequest request,
            @RequestParam(value = "check", required = false) String check) {
        if (check != null) {
            request.getSession(true).setAttribute("concordo", true);
            return "redirect:/login";
        }
        addError(model, "check", "Você precisa marcar que concorda");
        return "index";
    }

    @RequestMapping("/invalido")
    public String dadosCentral(Usuario usuario) {
        return "invalido/invalido";
    }

    private void addError(Model model, String campo, String mensagem) {
        DirectFieldBindingResult result = new DirectFieldBindingResult("", campo);
        result.addError(new ObjectError(campo, mensagem));
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + campo, result);
    }

}
